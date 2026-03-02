import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type {
  AiCostSummary,
  AiUsageHistory,
  ApiResponse,
  Breach,
  Commentary,
  CreateTradeRequest,
  PagedResponse,
  Trade,
} from '../types';
import { refreshToken, triggerReauthentication } from '../auth/keycloak';

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  metadata?: { requestId: string };
  _retryAuthHandled?: boolean;
};

const pendingControllers = new Map<string, AbortController>();
let reauthPromise: Promise<void> | null = null;

export const gatewayClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

const nextRequestId = () => crypto.randomUUID();

export const cancelActiveRequests = () => {
  for (const controller of pendingControllers.values()) {
    controller.abort();
  }
  pendingControllers.clear();
};

export const resetApiAuthState = () => {
  reauthPromise = null;
};

const startReauthOnce = async () => {
  if (!reauthPromise) {
    reauthPromise = triggerReauthentication().finally(() => {
      reauthPromise = null;
    });
  }
  await reauthPromise;
};

const setAuthorizationHeader = (config: RequestConfigWithMeta, token: string) => {
  if (!config.headers) {
    config.headers = {} as never;
  }
  (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
};

const attachAuthInterceptor = (client: AxiosInstance) => {
  client.interceptors.request.use(async (config: RequestConfigWithMeta) => {
    const token = await refreshToken();
    const requestId = nextRequestId();
    const controller = new AbortController();
    pendingControllers.set(requestId, controller);

    config.metadata = { requestId };
    config.signal = controller.signal;

    if (token) {
      setAuthorizationHeader(config, token);
    } else if (!config._retryAuthHandled && config.headers && 'Authorization' in config.headers) {
      delete (config.headers as Record<string, string>).Authorization;
    }
    return config;
  });
};

const unwrapClient = (name: string, client: AxiosInstance) => {
  (
    client.interceptors.response.use as unknown as (
      onFulfilled: (response: { data: unknown; config: RequestConfigWithMeta }) => unknown,
      onRejected: (error: AxiosError<{ message?: string; success?: boolean }>) => Promise<never>,
    ) => void
  )(
    (response) => {
      const config = response.config as RequestConfigWithMeta;
      const requestId = config.metadata?.requestId;
      if (requestId) {
        pendingControllers.delete(requestId);
      }

      const payload = response.data as ApiResponse<unknown> | unknown;
      if (payload && typeof payload === 'object' && 'data' in (payload as Record<string, unknown>)) {
        const typed = payload as ApiResponse<unknown>;
        if (typed.success === false) {
          throw new Error(typed.message || `Failed ${name} API request`);
        }
        return typed.data as unknown;
      }
      return payload as unknown;
    },
    async (error: AxiosError<{ message?: string; success?: boolean }>) => {
      if (error.code === 'ERR_CANCELED') {
        return Promise.reject(error);
      }

      const config = (error.config ?? {}) as RequestConfigWithMeta;
      const requestId = config.metadata?.requestId;
      if (requestId) {
        pendingControllers.delete(requestId);
      }

      if (error.response?.status === 401 && !config._retryAuthHandled) {
        config._retryAuthHandled = true;
        const recoveredToken = await refreshToken();
        if (recoveredToken) {
          setAuthorizationHeader(config, recoveredToken);
          return client.request(config);
        }
        await startReauthOnce();
        const postReauthToken = await refreshToken();
        if (postReauthToken) {
          setAuthorizationHeader(config, postReauthToken);
          return client.request(config);
        }
      }

      const envelopeMessage =
        error.response?.data &&
        typeof error.response.data === 'object' &&
        'message' in error.response.data
          ? error.response.data.message
          : undefined;
      const message = envelopeMessage || error.message || `Failed ${name} API request`;
      console.error(`[${name}] API error:`, message, error);
      return Promise.reject(new Error(message));
    },
  );
};

attachAuthInterceptor(gatewayClient);
unwrapClient('gateway', gatewayClient);

const normalizeList = <T>(payload: T[] | PagedResponse<T>) =>
  Array.isArray(payload) ? payload : payload.content;

const getUnwrapped = <T>(client: AxiosInstance, url: string) =>
  client.get(url).then((payload) => payload as T);

const postUnwrapped = <T>(client: AxiosInstance, url: string, body: unknown) =>
  client.post(url, body).then((payload) => payload as T);

export const tradeApi = {
  list: () =>
    getUnwrapped<Trade[] | PagedResponse<Trade>>(gatewayClient, '/api/v1/trades').then(normalizeList),
  getById: (id: string) => getUnwrapped<Trade>(gatewayClient, `/api/v1/trades/${id}`),
  create: (data: CreateTradeRequest) =>
    postUnwrapped<Trade>(gatewayClient, '/api/v1/trades', data),
};

export const breachApi = {
  list: () =>
    getUnwrapped<Breach[] | PagedResponse<Breach>>(gatewayClient, '/api/v1/breaches').then(
      normalizeList,
    ),
  getById: (id: string) => getUnwrapped<Breach>(gatewayClient, `/api/v1/breaches/${id}`),
};

export const commentaryApi = {
  list: (page = 0, size = 20) =>
    getUnwrapped<PagedResponse<Commentary>>(
      gatewayClient,
      `/api/v1/commentaries?page=${page}&size=${size}`,
    ),
  getById: (id: string) =>
    getUnwrapped<Commentary>(gatewayClient, `/api/v1/commentaries/${id}`),
  getByBreachId: (breachId: string) =>
    getUnwrapped<Commentary>(gatewayClient, `/api/v1/commentaries/breach/${breachId}`),
  approve: (id: string, approvedBy: string) =>
    postUnwrapped<Commentary>(gatewayClient, `/api/v1/commentaries/${id}/approve`, {
      approvedBy,
    }),
  getDailyCost: () =>
    getUnwrapped<AiCostSummary>(gatewayClient, '/api/v1/ai/cost/today'),
  getUsageHistory: (days = 30, recentLimit = 20) =>
    getUnwrapped<AiUsageHistory>(
      gatewayClient,
      `/api/v1/ai/usage/history?days=${days}&recentLimit=${recentLimit}`,
    ),
  getCircuitBreaker: () =>
    getUnwrapped<Record<string, unknown>>(gatewayClient, '/api/v1/ai/circuit-breaker'),
};

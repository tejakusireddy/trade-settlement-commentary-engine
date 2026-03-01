import axios, { type AxiosInstance } from 'axios';
import type {
  AiCostSummary,
  ApiResponse,
  Breach,
  Commentary,
  PagedResponse,
  Trade,
} from '../types';

const tradeClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082',
});

const breachClient = axios.create({
  baseURL: import.meta.env.VITE_BREACH_API_URL || 'http://localhost:8083',
});

const commentaryClient = axios.create({
  baseURL: import.meta.env.VITE_COMMENTARY_API_URL || 'http://localhost:8084',
});

const aiClient = axios.create({
  baseURL: import.meta.env.VITE_AI_API_URL || 'http://localhost:8084',
});

const attachErrorInterceptor = (clientName: string, client: AxiosInstance) => {
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      // Centralized API error logging for local debugging.
      console.error(`[${clientName}] API error`, error);
      throw error;
    },
  );
};

attachErrorInterceptor('trade', tradeClient);
attachErrorInterceptor('breach', breachClient);
attachErrorInterceptor('commentary', commentaryClient);
attachErrorInterceptor('ai', aiClient);

export const tradeApi = {
  get: async () => {
    const response = await tradeClient.get<ApiResponse<PagedResponse<Trade>>>('/api/v1/trades');
    return response.data;
  },
  create: async (payload: Partial<Trade>) => {
    const response = await tradeClient.post<ApiResponse<Trade>>('/api/v1/trades', payload);
    return response.data;
  },
  batch: async (payload: Partial<Trade>[]) => {
    const response = await tradeClient.post<ApiResponse<Trade[]>>('/api/v1/trades/batch', payload);
    return response.data;
  },
};

export const breachApi = {
  list: async () => {
    const response = await breachClient.get<ApiResponse<PagedResponse<Breach> | Breach[]>>('/api/v1/breaches');
    return response.data;
  },
  getById: async (id: string) => {
    const response = await breachClient.get<ApiResponse<Breach>>(`/api/v1/breaches/${id}`);
    return response.data;
  },
};

export const commentaryApi = {
  list: async (page = 0, size = 20) => {
    const response = await commentaryClient.get<ApiResponse<PagedResponse<Commentary>>>(
      `/api/v1/commentaries?page=${page}&size=${size}`,
    );
    return response.data;
  },
  getById: async (id: string) => {
    const response = await commentaryClient.get<ApiResponse<Commentary>>(`/api/v1/commentaries/${id}`);
    return response.data;
  },
  getByBreachId: async (breachId: string) => {
    const response = await commentaryClient.get<ApiResponse<Commentary>>(
      `/api/v1/commentaries/breach/${breachId}`,
    );
    return response.data;
  },
  approve: async (id: string, approvedBy: string) => {
    const response = await commentaryClient.post<ApiResponse<Commentary>>(
      `/api/v1/commentaries/${id}/approve`,
      { approvedBy },
    );
    return response.data;
  },
};

export const aiApi = {
  getCostToday: async () => {
    const response = await aiClient.get<ApiResponse<AiCostSummary & Record<string, unknown>>>('/api/v1/ai/cost/today');
    return response.data;
  },
  getCircuitBreaker: async () => {
    const response = await aiClient.get<ApiResponse<Record<string, unknown>>>('/api/v1/ai/circuit-breaker');
    return response.data;
  },
};

import Keycloak, { type KeycloakConfig, type KeycloakTokenParsed } from 'keycloak-js';

const keycloakConfig: KeycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'trade-settlement',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'trade-api',
};

export const keycloak = new Keycloak(keycloakConfig);

let initialized = false;
let initPromise: Promise<boolean> | null = null;
let reauthInProgress = false;
const SWITCH_USER_QUERY_PARAM = 'kc_action';
const SWITCH_USER_QUERY_VALUE = 'switch-user';

const clearAuthStorage = () => {
  const clearByPrefix = (storage: Storage, prefixes: string[]) => {
    for (let i = storage.length - 1; i >= 0; i -= 1) {
      const key = storage.key(i);
      if (!key) {
        continue;
      }
      if (prefixes.some((prefix) => key.toLowerCase().startsWith(prefix))) {
        storage.removeItem(key);
      }
    }
  };

  clearByPrefix(window.localStorage, ['kc-', 'keycloak']);
  clearByPrefix(window.sessionStorage, ['kc-', 'keycloak']);
};

const buildRedirectUri = (forcePromptLogin: boolean): string => {
  if (!forcePromptLogin) {
    return window.location.origin;
  }
  const url = new URL(window.location.origin);
  url.searchParams.set(SWITCH_USER_QUERY_PARAM, SWITCH_USER_QUERY_VALUE);
  return url.toString();
};

const clearLocalAuthState = () => {
  keycloak.clearToken();
  clearAuthStorage();
};

export const initKeycloak = async (): Promise<boolean> => {
  if (initialized) {
    return Boolean(keycloak.authenticated);
  }
  if (initPromise) {
    return initPromise;
  }

  initPromise = (async () => {
    try {
      const url = new URL(window.location.href);
      const forcePromptLogin =
        url.searchParams.get(SWITCH_USER_QUERY_PARAM) === SWITCH_USER_QUERY_VALUE;
      if (forcePromptLogin) {
        url.searchParams.delete(SWITCH_USER_QUERY_PARAM);
        window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
      }

      const authenticated = await keycloak.init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        ...(forcePromptLogin ? { prompt: 'login' } : {}),
      });
      initialized = true;
      return authenticated;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (message.includes('can only be initialized once')) {
        initialized = true;
        return Boolean(keycloak.authenticated);
      }
      console.error('Failed to initialize Keycloak', error);
      return false;
    } finally {
      initPromise = null;
    }
  })();

  return initPromise;
};

export const login = async (): Promise<void> => {
  await keycloak.login();
};

export const loginWithPrompt = async (): Promise<void> => {
  await keycloak.login({ prompt: 'login', maxAge: 0 });
};

const performLogout = async (forcePromptLogin: boolean): Promise<void> => {
  const redirectUri = buildRedirectUri(forcePromptLogin);
  try {
    await keycloak.logout({ redirectUri });
  } finally {
    clearLocalAuthState();
  }
};

export const logout = async (): Promise<void> => {
  await performLogout(false);
};

export const switchUser = async (): Promise<void> => {
  await performLogout(true);
};

export const getAccessToken = (): string | undefined => keycloak.token;

export const refreshToken = async (): Promise<string | undefined> => {
  const existingToken = keycloak.token;
  if (!keycloak.authenticated) {
    return existingToken;
  }
  try {
    await keycloak.updateToken(30);
    return keycloak.token ?? existingToken;
  } catch (error) {
    // Fail soft on transient refresh errors and let API 401 handling drive re-auth.
    console.warn('Token refresh failed, continuing with current token if present', error);
    return keycloak.token ?? existingToken;
  }
};

const getTokenParsed = (): KeycloakTokenParsed | undefined =>
  keycloak.tokenParsed as KeycloakTokenParsed | undefined;

export const getUserRoles = (): string[] => {
  const parsed = getTokenParsed();
  if (!parsed) {
    return [];
  }

  const realmRoles = parsed.realm_access?.roles ?? [];
  const resourceRoles = Object.values(parsed.resource_access ?? {})
    .flatMap((entry) => entry.roles ?? []);

  return [...new Set([...realmRoles, ...resourceRoles])];
};

export const getUsername = (): string =>
  getTokenParsed()?.preferred_username || getTokenParsed()?.sub || 'unknown-user';

export const getSessionKey = (): string => {
  const parsed = getTokenParsed();
  const sub = parsed?.sub ?? 'anonymous';
  const sessionState = parsed?.session_state || parsed?.sid || parsed?.iat || 0;
  return `${sub}:${sessionState}`;
};

export const triggerReauthentication = async (): Promise<void> => {
  if (reauthInProgress) {
    return;
  }
  reauthInProgress = true;
  try {
    await loginWithPrompt();
  } finally {
    reauthInProgress = false;
  }
};

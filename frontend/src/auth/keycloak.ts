import Keycloak, { type KeycloakConfig, type KeycloakTokenParsed } from 'keycloak-js';

const keycloakConfig: KeycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'trade-settlement',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'trade-api',
};

export const keycloak = new Keycloak(keycloakConfig);

let initialized = false;
let reauthInProgress = false;

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

export const initKeycloak = async (): Promise<boolean> => {
  if (initialized) {
    return Boolean(keycloak.authenticated);
  }

  try {
    const authenticated = await keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
    initialized = true;
    return authenticated;
  } catch (error) {
    console.error('Failed to initialize Keycloak', error);
    return false;
  }
};

export const login = async (): Promise<void> => {
  await keycloak.login();
};

export const loginWithPrompt = async (): Promise<void> => {
  await keycloak.login({ prompt: 'login', maxAge: 0 });
};

export const logout = async (): Promise<void> => {
  keycloak.clearToken();
  clearAuthStorage();
  await keycloak.logout({ redirectUri: window.location.origin });
};

export const getAccessToken = (): string | undefined => keycloak.token;

export const refreshToken = async (): Promise<string | undefined> => {
  if (!keycloak.authenticated) {
    return undefined;
  }
  try {
    await keycloak.updateToken(30);
    return keycloak.token;
  } catch (error) {
    console.warn('Token refresh failed, clearing token', error);
    keycloak.clearToken();
    return undefined;
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

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { SkeletonLoader } from '../components/ui/SkeletonLoader';
import { useAppStore } from '../store/appStore';
import { cancelActiveRequests, resetApiAuthState } from '../services/api';
import {
  getSessionKey,
  getUserRoles,
  getUsername,
  initKeycloak,
  keycloak,
  login,
  logout,
  switchUser as switchKeycloakUser,
} from './keycloak';

interface AuthContextValue {
  authenticated: boolean;
  username: string;
  sessionKey: string;
  roles: string[];
  isAdmin: boolean;
  canApprove: boolean;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  switchUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const resetAppState = useAppStore((state) => state.resetAppState);
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [username, setUsername] = useState('unknown-user');
  const [sessionKey, setSessionKey] = useState('anonymous:0');
  const [roles, setRoles] = useState<string[]>([]);
  const previousSessionKeyRef = useRef<string | null>(null);

  const clearClientState = useCallback(async () => {
    cancelActiveRequests();
    await queryClient.cancelQueries();
    queryClient.clear();
    resetAppState();
    resetApiAuthState();
  }, [queryClient, resetAppState]);

  const syncAuthState = () => {
    setAuthenticated(Boolean(keycloak.authenticated));
    setUsername(getUsername());
    setRoles(getUserRoles());
    setSessionKey(getSessionKey());
  };

  useEffect(() => {
    const bootstrapAuth = async () => {
      const ok = await initKeycloak();
      if (ok) {
        syncAuthState();
      }
      setLoading(false);
    };
    void bootstrapAuth();
  }, []);

  useEffect(() => {
    const onAuthChange = () => {
      syncAuthState();
    };
    const onAuthLogout = () => {
      setAuthenticated(false);
      setUsername('unknown-user');
      setRoles([]);
      setSessionKey('anonymous:0');
      previousSessionKeyRef.current = null;
      void clearClientState();
    };

    keycloak.onAuthSuccess = onAuthChange;
    keycloak.onAuthRefreshSuccess = onAuthChange;
    keycloak.onTokenExpired = onAuthChange;
    keycloak.onAuthLogout = onAuthLogout;
  }, [clearClientState]);

  useEffect(() => {
    if (!authenticated) {
      previousSessionKeyRef.current = null;
      return;
    }
    if (previousSessionKeyRef.current === null) {
      previousSessionKeyRef.current = sessionKey;
      return;
    }
    if (previousSessionKeyRef.current !== sessionKey) {
      previousSessionKeyRef.current = sessionKey;
      void clearClientState();
      return;
    }
    previousSessionKeyRef.current = sessionKey;
  }, [authenticated, clearClientState, sessionKey]);

  const value = useMemo<AuthContextValue>(
    () => ({
      authenticated,
      username,
      sessionKey,
      roles,
      isAdmin: roles.includes('admin'),
      canApprove: roles.includes('admin') || roles.includes('compliance-officer'),
      login,
      logout: async () => {
        await clearClientState();
        await logout();
      },
      switchUser: async () => {
        await clearClientState();
        await switchKeycloakUser();
      },
    }),
    [authenticated, clearClientState, roles, sessionKey, username],
  );

  if (loading) {
    return (
      <div className="bg-bg-base p-6">
        <div className="mb-4 grid grid-cols-3 gap-4">
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
        </div>
        <SkeletonLoader variant="row" className="mb-3" />
        <SkeletonLoader variant="row" className="mb-3" />
        <SkeletonLoader variant="row" />
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-base">
        <div className="rounded-lg border border-border-subtle bg-bg-surface p-6 text-center">
          <h1 className="mb-2 text-base font-medium text-text-primary">Session Required</h1>
          <p className="mb-4 text-sm text-text-secondary">
            Sign in with Keycloak to access the settlement console.
          </p>
          <button
            type="button"
            onClick={() => void login()}
            className="rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-sm text-primary transition-colors hover:bg-primary hover:text-black"
          >
            Sign In
          </button>
        </div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

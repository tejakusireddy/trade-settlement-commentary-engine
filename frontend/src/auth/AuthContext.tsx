import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
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
  loginWithPrompt,
  logout,
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

  const clearClientState = async () => {
    cancelActiveRequests();
    await queryClient.cancelQueries();
    queryClient.clear();
    resetAppState();
    resetApiAuthState();
  };

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
    };

    keycloak.onAuthSuccess = onAuthChange;
    keycloak.onAuthRefreshSuccess = onAuthChange;
    keycloak.onTokenExpired = onAuthChange;
    keycloak.onAuthLogout = onAuthLogout;
  }, []);

  useEffect(() => {
    if (!authenticated) {
      return;
    }
    void clearClientState();
  }, [sessionKey]);

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
        await loginWithPrompt();
      },
    }),
    [authenticated, roles, sessionKey, username],
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

"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type Keycloak from "keycloak-js";

import { clearAccessToken, setAccessToken } from "@/lib/authTokenStore";
import { getKeycloakClient, initKeycloak } from "@/lib/keycloakClient";

type AuthContextValue = {
  initialized: boolean;
  authenticated: boolean;
  loggingOut: boolean;
  keycloak: Keycloak | null;
  token: string | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<string | null>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);

  const syncToken = useCallback((nextToken: string | null) => {
    setToken(nextToken);
    setAccessToken(nextToken);
  }, []);

  const refreshToken = useCallback(async () => {
    const client = getKeycloakClient();

    if (!client.authenticated) {
      syncToken(null);
      return null;
    }

    try {
      await client.updateToken(60);

      const nextToken = client.token ?? null;
      syncToken(nextToken);

      return nextToken;
    } catch {
      syncToken(null);
      setAuthenticated(false);

      return null;
    }
  }, [syncToken]);

  const login = useCallback(async () => {
    const client = getKeycloakClient();

    await client.login({
      redirectUri: window.location.origin,
    });
  }, []);

  const logout = useCallback(async () => {
    const client = getKeycloakClient();

    setLoggingOut(true);
    clearAccessToken();
    syncToken(null);

    try {
      await client.logout({
        redirectUri: `${window.location.origin}/login`,
      });
    } catch (error) {
      console.error("Could not logout from Keycloak", error);

      setAuthenticated(false);
      setLoggingOut(false);

      window.location.assign("/login");
    }
  }, [syncToken]);

  useEffect(() => {
    let mounted = true;
    let refreshIntervalId: number | undefined;

    async function initializeAuth() {
      try {
        const client = getKeycloakClient();
        const isAuthenticated = await initKeycloak();

        if (!mounted) {
          return;
        }

        setKeycloak(client);
        setAuthenticated(isAuthenticated);
        syncToken(client.token ?? null);

        client.onTokenExpired = () => {
          void refreshToken();
        };

        refreshIntervalId = window.setInterval(() => {
          void refreshToken();
        }, 30_000);
      } catch (error) {
        console.error("Could not initialize Keycloak", error);

        if (mounted) {
          clearAccessToken();
          syncToken(null);
          setAuthenticated(false);
        }
      } finally {
        if (mounted) {
          setInitialized(true);
        }
      }
    }

    void initializeAuth();

    return () => {
      mounted = false;

      if (refreshIntervalId) {
        window.clearInterval(refreshIntervalId);
      }
    };
  }, [refreshToken, syncToken]);

  const value = useMemo<AuthContextValue>(
    () => ({
      initialized,
      authenticated,
      loggingOut,
      keycloak,
      token,
      login,
      logout,
      refreshToken,
    }),
    [
      authenticated,
      initialized,
      keycloak,
      loggingOut,
      login,
      logout,
      refreshToken,
      token,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}

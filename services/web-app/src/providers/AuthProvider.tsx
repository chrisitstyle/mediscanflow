"use client";

import { createContext, useContext, type ReactNode } from "react";

import {
  useKeycloakSession,
  type KeycloakSession,
} from "@/hooks/useKeycloakSession";

const AuthContext = createContext<KeycloakSession | null>(null);

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const session = useKeycloakSession();

  return (
    <AuthContext.Provider value={session}>{children}</AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}

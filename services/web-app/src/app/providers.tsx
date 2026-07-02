"use client";

import type { ReactNode } from "react";

import { QueryProvider } from "@/lib/queryProvider";
import { AuthProvider } from "@/providers/AuthProvider";

type ProvidersProps = {
  children: ReactNode;
};

export function Providers({ children }: ProvidersProps) {
  return (
    <AuthProvider>
      <QueryProvider>{children}</QueryProvider>
    </AuthProvider>
  );
}

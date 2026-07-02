"use client";

import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

import { useAuth } from "@/providers/AuthProvider";

import { Skeleton } from "@/components/ui/skeleton";

type ProtectedAppProps = {
  children: ReactNode;
};

const PUBLIC_PATHS = ["/login"];

export function ProtectedApp({ children }: ProtectedAppProps) {
  const pathname = usePathname();
  const { initialized, authenticated, login } = useAuth();

  const isPublicPath = PUBLIC_PATHS.some((path) => pathname.startsWith(path));

  if (!initialized) {
    return (
      <div className="mx-auto flex min-h-screen w-full max-w-6xl flex-col gap-6 px-6 py-8">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!authenticated && !isPublicPath) {
    void login();

    return (
      <main className="flex min-h-screen items-center justify-center px-6">
        <div className="text-center">
          <p className="text-sm text-muted-foreground">
            Redirecting to login...
          </p>
        </div>
      </main>
    );
  }

  return children;
}

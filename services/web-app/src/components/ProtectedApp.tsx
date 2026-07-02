"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";

import { AuthStatusScreen } from "@/components/AuthStatusScreen";
import { useAuth } from "@/providers/AuthProvider";

type ProtectedAppProps = {
  children: ReactNode;
};

const PUBLIC_PATHS = ["/login"];

export function ProtectedApp({ children }: ProtectedAppProps) {
  const pathname = usePathname();
  const router = useRouter();

  const { initialized, authenticated, loggingOut } = useAuth();

  const isPublicPath = PUBLIC_PATHS.some((path) => pathname.startsWith(path));

  useEffect(() => {
    if (!initialized || authenticated || isPublicPath || loggingOut) {
      return;
    }

    const redirectTo = `${window.location.pathname}${window.location.search}`;

    if (redirectTo === "/") {
      router.replace("/login");
      return;
    }

    router.replace(`/login?redirectTo=${encodeURIComponent(redirectTo)}`);
  }, [authenticated, initialized, isPublicPath, loggingOut, router]);

  if (!initialized) {
    return (
      <AuthStatusScreen
        title="Preparing secure workspace"
        description="MediScanFlow is checking your session before loading patient data."
      />
    );
  }

  if (loggingOut) {
    return (
      <AuthStatusScreen
        title="Signing out"
        description="You are being securely signed out from MediScanFlow."
      />
    );
  }

  if (!authenticated && !isPublicPath) {
    return (
      <AuthStatusScreen
        title="Opening sign in page"
        description="You need to sign in with your organization account to continue."
      />
    );
  }

  return children;
}

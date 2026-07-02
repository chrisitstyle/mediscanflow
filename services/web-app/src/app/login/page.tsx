"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Activity,
  Brain,
  Loader2,
  LockKeyhole,
  ScanLine,
  ShieldCheck,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useAuth } from "@/providers/AuthProvider";

const highlights = [
  {
    icon: ScanLine,
    label: "Medical scan workflows",
  },
  {
    icon: Brain,
    label: "AI-assisted analysis",
  },
  {
    icon: Activity,
    label: "Processing status tracking",
  },
];

function getSafeRedirectTo(value: string | null) {
  if (!value) {
    return "/";
  }

  if (!value.startsWith("/") || value.startsWith("//")) {
    return "/";
  }

  return value;
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <LoginPageView isPreparing authenticated={false} onLogin={undefined} />
      }
    >
      <LoginPageContent />
    </Suspense>
  );
}

function LoginPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { initialized, authenticated, login } = useAuth();

  const redirectTo = getSafeRedirectTo(searchParams.get("redirectTo"));

  useEffect(() => {
    if (initialized && authenticated) {
      router.replace(redirectTo);
    }
  }, [authenticated, initialized, redirectTo, router]);

  async function handleLogin() {
    await login(`${window.location.origin}${redirectTo}`);
  }

  return (
    <LoginPageView
      isPreparing={!initialized}
      authenticated={authenticated}
      onLogin={() => void handleLogin()}
    />
  );
}

type LoginPageViewProps = {
  isPreparing: boolean;
  authenticated: boolean;
  onLogin?: () => void;
};

function LoginPageView({
  isPreparing,
  authenticated,
  onLogin,
}: LoginPageViewProps) {
  return (
    <main className="relative flex min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.18),transparent_32%),linear-gradient(to_bottom,hsl(var(--background)),hsl(var(--muted)/0.45))] px-6 py-10">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-linear-to-r from-transparent via-primary/40 to-transparent" />

      <div className="mx-auto grid w-full max-w-6xl items-center gap-10 lg:grid-cols-[1.05fr_0.95fr]">
        <section className="hidden lg:block">
          <div className="inline-flex items-center gap-2 rounded-full border border-border/70 bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground shadow-sm backdrop-blur">
            <ShieldCheck className="size-3.5 text-primary" />
            Secure healthcare AI platform
          </div>

          <h1 className="mt-6 max-w-2xl text-5xl font-bold tracking-tight">
            MediScan
            <span className="text-primary">Flow</span>
          </h1>

          <p className="mt-5 max-w-xl text-lg text-muted-foreground">
            Manage patients, upload medical scans and monitor AI analysis
            results from one protected workspace.
          </p>

          <div className="mt-8 grid max-w-xl gap-3">
            {highlights.map((item) => {
              const Icon = item.icon;

              return (
                <div
                  key={item.label}
                  className="flex items-center gap-3 rounded-2xl border border-border/70 bg-background/70 p-4 shadow-sm backdrop-blur"
                >
                  <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <Icon className="size-5" />
                  </div>

                  <span className="text-sm font-medium">{item.label}</span>
                </div>
              );
            })}
          </div>
        </section>

        <section className="mx-auto w-full max-w-md">
          <Card className="border-border/70 bg-background/90 shadow-xl backdrop-blur">
            <CardHeader className="text-center">
              <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-sm shadow-primary/25">
                <ScanLine className="size-7" aria-hidden="true" />
              </div>

              <CardTitle className="text-2xl">
                Sign in to MediScanFlow
              </CardTitle>

              <CardDescription>
                Use your organization account to access patient scan analysis
                workflows.
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-4">
              <Button
                type="button"
                className="h-11 w-full"
                disabled={isPreparing || authenticated}
                onClick={onLogin}
              >
                {isPreparing ? (
                  <>
                    <Loader2 className="size-4 animate-spin" />
                    Preparing sign in...
                  </>
                ) : authenticated ? (
                  <>
                    <Loader2 className="size-4 animate-spin" />
                    Opening workspace...
                  </>
                ) : (
                  <>
                    <LockKeyhole className="size-4" />
                    Continue with Keycloak
                  </>
                )}
              </Button>

              <p className="text-center text-xs text-muted-foreground">
                Access is restricted to authorized MediScanFlow users.
              </p>
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  );
}

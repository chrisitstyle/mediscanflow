"use client";

import { ShieldCheck } from "lucide-react";
import { useAuth } from "@/providers/AuthProvider";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function LoginPage() {
  const { initialized, authenticated, login } = useAuth();

  if (initialized && authenticated) {
    window.location.href = "/";
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/30 px-6 py-10">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex size-12 items-center justify-center rounded-full bg-primary/10">
            <ShieldCheck className="size-6 text-primary" />
          </div>

          <CardTitle className="text-2xl">Sign in to MediScanFlow</CardTitle>

          <CardDescription>
            Use your organization account to access patient scan analysis
            workflows.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          <Button
            type="button"
            className="w-full"
            disabled={!initialized}
            onClick={() => void login()}
          >
            Continue with Keycloak
          </Button>

          <p className="text-center text-xs text-muted-foreground">
            Access is restricted to authorized MediScanFlow users.
          </p>
        </CardContent>
      </Card>
    </main>
  );
}

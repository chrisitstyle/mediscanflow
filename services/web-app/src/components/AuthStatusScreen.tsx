import { Loader2, ScanLine, ShieldCheck } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

type AuthStatusScreenProps = {
  title: string;
  description: string;
  showSpinner?: boolean;
  actionLabel?: string;
  onAction?: () => void;
};

export function AuthStatusScreen({
  title,
  description,
  showSpinner = true,
  actionLabel,
  onAction,
}: AuthStatusScreenProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,hsl(var(--primary)/0.14),transparent_34%),linear-gradient(to_bottom,hsl(var(--background)),hsl(var(--muted)/0.35))] px-6 py-10">
      <Card className="w-full max-w-md border-border/70 bg-background/90 shadow-xl backdrop-blur">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-sm shadow-primary/25">
            <ScanLine className="size-7" aria-hidden="true" />
          </div>

          <div className="flex items-center justify-center gap-2">
            <ShieldCheck className="size-4 text-primary" />
            <span className="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
              Secure access
            </span>
          </div>

          <CardTitle className="mt-3 text-2xl">{title}</CardTitle>

          <CardDescription className="mx-auto max-w-sm">
            {description}
          </CardDescription>
        </CardHeader>

        <CardContent className="flex flex-col items-center gap-4">
          {showSpinner && (
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          )}

          {actionLabel && onAction && (
            <Button type="button" className="w-full" onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </CardContent>
      </Card>
    </main>
  );
}

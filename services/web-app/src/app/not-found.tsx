import Link from "next/link";

import { ArrowLeft, SearchX } from "lucide-react";

import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <main className="flex min-h-[calc(100dvh-4rem)] items-center justify-center px-6 py-16">
      <div className="w-full max-w-md text-center">
        <div className="relative mx-auto mb-8 flex size-28 items-center justify-center rounded-3xl border bg-card shadow-sm">
          <SearchX
            className="size-12 text-muted-foreground"
            aria-hidden="true"
          />

          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-x-3 top-1/2 h-px animate-pulse bg-chart-4/70 motion-reduce:animate-none"
          />
        </div>

        <p className="mb-2 font-mono text-sm font-medium text-chart-4">
          Error 404
        </p>

        <h1 className="text-balance text-3xl font-semibold tracking-tight">
          Page not found
        </h1>

        <p className="mt-3 text-pretty leading-relaxed text-muted-foreground">
          This page doesn&apos;t exist or may have been moved. Check the address
          or head back to your dashboard to keep working.
        </p>

        <div className="mt-8 flex justify-center">
          <Button asChild>
            <Link href="/">
              <ArrowLeft className="size-4" aria-hidden="true" />
              Back to dashboard
            </Link>
          </Button>
        </div>
      </div>
    </main>
  );
}

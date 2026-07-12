import Link from "next/link";
import { ArrowLeft, ScanLine, SearchX } from "lucide-react";

import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <main className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-6 py-16">
      <div className="w-full max-w-md text-center">
        {/* Brand mark */}
        <div className="mb-8 flex items-center justify-center gap-2 text-muted-foreground">
          <ScanLine className="size-5" aria-hidden="true" />
          <span className="text-sm font-medium tracking-wide">MediScanFlow</span>
        </div>

        {/* Scanner-style graphic */}
        <div className="relative mx-auto mb-8 flex size-28 items-center justify-center rounded-3xl border border-border bg-card shadow-sm">
          <SearchX
            className="size-12 text-muted-foreground"
            aria-hidden="true"
          />
          <span
            className="pointer-events-none absolute inset-x-3 top-1/2 h-px animate-pulse bg-chart-4/70"
            aria-hidden="true"
          />
        </div>

        <p className="mb-2 font-mono text-sm font-medium text-chart-4">
          Error 404
        </p>
        <h1 className="text-balance text-3xl font-semibold tracking-tight text-foreground">
          Nie znaleziono strony
        </h1>
        <p className="mt-3 text-pretty leading-relaxed text-muted-foreground">
          {
            "Ta strona nie istnieje lub została przeniesiona. Sprawdź adres albo wróć do panelu, aby kontynuować pracę."
          }
        </p>

        <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Button asChild>
            <Link href="/">
              <ArrowLeft className="size-4" aria-hidden="true" />
              Wróć do panelu
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link href="/analyses">Zobacz analizy</Link>
          </Button>
        </div>
      </div>
    </main>
  );
}

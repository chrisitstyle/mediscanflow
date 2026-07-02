"use client";

import { Badge } from "@/components/ui/badge";
import { AuditTimeline } from "@/features/audit/AuditTimeline";

export function ActivityPage() {
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <div>
        <Badge variant="secondary">Activity</Badge>

        <h1 className="mt-4 text-3xl font-bold tracking-tight">Activity log</h1>

        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Review recent audit events and user actions across MediScanFlow.
        </p>
      </div>

      <AuditTimeline
        scope="recent"
        limit={50}
        title="Audit trail"
        description="Latest recorded user actions across patients, analyses and reports."
        emptyTitle="No activity yet"
        emptyDescription="Audit events will appear here after users create patients, update profiles, upload scans, retry analyses or download reports."
        refetchInterval={15_000}
        showLinks
      />
    </main>
  );
}

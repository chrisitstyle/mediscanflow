"use client";

import { AuditTimeline } from "@/features/audit/AuditTimeline";

export function RecentActivityCard() {
  return (
    <AuditTimeline
      scope="recent"
      limit={10}
      title="Recent activity"
      description="Latest user actions and audit events across the platform."
      emptyTitle="No recent activity"
      emptyDescription="Audit events will appear here after users create patients, upload scans, retry analyses, or download reports."
      refetchInterval={15_000}
      showLinks
    />
  );
}

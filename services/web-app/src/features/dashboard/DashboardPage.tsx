"use client";

import { Badge } from "@/components/ui/badge";
import { AnalysesOverTimeChartCard } from "@/features/dashboard/AnalysesOverTimeChartCard";
import { AnalysisStatusChartCard } from "@/features/dashboard/AnalysisStatusChartCard";
import { QuickActionsCard } from "@/features/dashboard/QuickActionsCard";
import { RecentAnalysesCard } from "@/features/dashboard/RecentAnalysesCard";
import { SummaryCards } from "@/features/dashboard/SummaryCards";
import { SystemStatusCard } from "@/features/dashboard/SystemStatusCard";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { canViewSystemStatus } from "@/lib/permissions";

export function DashboardPage() {
  const currentUserQuery = useCurrentUser();
  const currentUser = currentUserQuery.data;

  const showSystemStatus =
    !currentUserQuery.isLoading && canViewSystemStatus(currentUser);

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <div>
        <Badge variant="secondary">Dashboard</Badge>

        <h1 className="mt-4 text-3xl font-bold tracking-tight">
          MediScanFlow overview
        </h1>

        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Monitor patients, scan analyses and AI processing status from one
          place.
        </p>
      </div>

      <SummaryCards />

      <div className="grid gap-6 lg:grid-cols-2">
        <AnalysisStatusChartCard />
        <AnalysesOverTimeChartCard days={14} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <RecentAnalysesCard />

        <div className="flex flex-col gap-6">
          {showSystemStatus && <SystemStatusCard />}
          <QuickActionsCard />
        </div>
      </div>
    </main>
  );
}

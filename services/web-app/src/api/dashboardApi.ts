import { apiFetch } from "@/lib/apiClient";
import type {
  AnalysisStatusCount,
  DailyAnalysisCount,
  DashboardSummary,
} from "@/types/dashboard";

export function getDashboardSummary(): Promise<DashboardSummary> {
  return apiFetch<DashboardSummary>("/dashboard/summary");
}

export function getAnalysisStatusBreakdown(): Promise<AnalysisStatusCount[]> {
  return apiFetch<AnalysisStatusCount[]>(
    "/dashboard/analysis-status-breakdown",
  );
}

export function getAnalysesOverTime(days = 14): Promise<DailyAnalysisCount[]> {
  const searchParams = new URLSearchParams({
    days: String(days),
  });

  return apiFetch<DailyAnalysisCount[]>(
    `/dashboard/analyses-over-time?${searchParams.toString()}`,
  );
}

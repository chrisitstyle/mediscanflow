"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useQuery } from "@tanstack/react-query";

import { getAnalysisStatusBreakdown } from "@/api/dashboardApi";
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";
import type { AnalysisStatus } from "@/types/dashboard";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

const STATUS_LABELS: Record<AnalysisStatus, string> = {
  UPLOADED: "Uploaded",
  QUEUED: "Queued",
  PROCESSING: "Processing",
  COMPLETED: "Completed",
  FAILED: "Failed",
};

const ANALYSIS_STATUS_CHART_COLORS: Record<AnalysisStatus, string> = {
  UPLOADED: "var(--chart-4)",
  QUEUED: "var(--chart-5)",
  PROCESSING: "var(--chart-2)",
  COMPLETED: "var(--chart-1)",
  FAILED: "var(--chart-3)",
};

export function AnalysisStatusChartCard() {
  const statusQuery = useQuery({
    queryKey: queryKeys.dashboard.analysisStatusBreakdown(),
    queryFn: getAnalysisStatusBreakdown,
  });

  const errorMessage =
    statusQuery.error instanceof ApiClientError
      ? statusQuery.error.message
      : "Could not load analysis status breakdown";

  const chartData =
    statusQuery.data?.map((item) => ({
      statusKey: item.status,
      status: STATUS_LABELS[item.status],
      count: item.count,
    })) ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Analyses by status</CardTitle>
        <CardDescription>
          Current distribution of AI analysis workflow states.
        </CardDescription>
      </CardHeader>

      <CardContent>
        {statusQuery.isLoading && (
          <div className="flex h-65 flex-col justify-end gap-3">
            <Skeleton className="h-40 w-full" />
            <Skeleton className="h-4 w-3/4" />
          </div>
        )}

        {statusQuery.isError && (
          <Alert variant="destructive">
            <AlertTitle>Could not load chart</AlertTitle>
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        {statusQuery.data && (
          <div className="h-65">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={chartData}
                margin={{
                  top: 8,
                  right: 8,
                  left: -20,
                  bottom: 0,
                }}
              >
                <CartesianGrid
                  vertical={false}
                  stroke="var(--border)"
                  strokeDasharray="3 3"
                />

                <XAxis
                  dataKey="status"
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  tick={{
                    fill: "var(--muted-foreground)",
                    fontSize: 12,
                  }}
                />

                <YAxis
                  allowDecimals={false}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  tick={{
                    fill: "var(--muted-foreground)",
                    fontSize: 12,
                  }}
                  domain={[0, (dataMax: number) => Math.max(1, dataMax)]}
                />

                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--popover)",
                    borderColor: "var(--border)",
                    color: "var(--popover-foreground)",
                    borderRadius: "0.75rem",
                    boxShadow: "var(--shadow-lg)",
                  }}
                  labelStyle={{
                    color: "var(--popover-foreground)",
                    fontWeight: 600,
                  }}
                  itemStyle={{
                    color: "var(--muted-foreground)",
                  }}
                  cursor={{
                    fill: "var(--muted)",
                  }}
                />

                <Bar dataKey="count" name="Analyses" radius={[6, 6, 0, 0]}>
                  {chartData.map((entry) => (
                    <Cell
                      key={entry.statusKey}
                      fill={ANALYSIS_STATUS_CHART_COLORS[entry.statusKey]}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

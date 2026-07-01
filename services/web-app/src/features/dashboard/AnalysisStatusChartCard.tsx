"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
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
                <CartesianGrid vertical={false} strokeDasharray="3 3" />
                <XAxis
                  dataKey="status"
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  tick={{ fontSize: 12 }}
                />
                <YAxis
                  allowDecimals={false}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  tick={{ fontSize: 12 }}
                  domain={[0, (dataMax: number) => Math.max(1, dataMax)]}
                />
                <Tooltip />
                <Bar
                  dataKey="count"
                  name="Analyses"
                  fill="var(--primary)"
                  radius={[6, 6, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

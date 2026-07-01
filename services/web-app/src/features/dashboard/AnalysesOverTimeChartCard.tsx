"use client";

import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useQuery } from "@tanstack/react-query";

import { getAnalysesOverTime } from "@/api/dashboardApi";
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

type AnalysesOverTimeChartCardProps = {
  days?: number;
};

function formatChartDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);

  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
  }).format(new Date(year, month - 1, day));
}

export function AnalysesOverTimeChartCard({
  days = 14,
}: AnalysesOverTimeChartCardProps) {
  const overTimeQuery = useQuery({
    queryKey: queryKeys.dashboard.analysesOverTime(days),
    queryFn: () => getAnalysesOverTime(days),
  });

  const errorMessage =
    overTimeQuery.error instanceof ApiClientError
      ? overTimeQuery.error.message
      : "Could not load analyses over time";

  const chartData =
    overTimeQuery.data?.map((item) => ({
      date: formatChartDate(item.date),
      count: item.count,
    })) ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Analyses over time</CardTitle>
        <CardDescription>
          Daily number of submitted scans over the last {days} days.
        </CardDescription>
      </CardHeader>

      <CardContent>
        {overTimeQuery.isLoading && (
          <div className="flex h-65 flex-col justify-end gap-3">
            <Skeleton className="h-40 w-full" />
            <Skeleton className="h-4 w-3/4" />
          </div>
        )}

        {overTimeQuery.isError && (
          <Alert variant="destructive">
            <AlertTitle>Could not load chart</AlertTitle>
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        {overTimeQuery.data && (
          <div className="h-65">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart
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
                  dataKey="date"
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
                <Line
                  type="monotone"
                  dataKey="count"
                  name="Analyses"
                  stroke="var(--primary)"
                  strokeWidth={2}
                  dot={{
                    r: 3,
                    fill: "var(--primary)",
                  }}
                  activeDot={{
                    r: 5,
                  }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

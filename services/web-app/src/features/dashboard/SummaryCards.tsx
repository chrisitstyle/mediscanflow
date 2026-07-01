"use client";

import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  FileImage,
  Users,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getDashboardSummary } from "@/api/dashboardApi";
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function formatCount(value: number | undefined) {
  if (value === undefined) {
    return "—";
  }

  return new Intl.NumberFormat("en").format(value);
}

export function SummaryCards() {
  const {
    data: summary,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: queryKeys.dashboard.summary(),
    queryFn: getDashboardSummary,
    refetchInterval: 15_000,
  });

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : "Could not load dashboard summary";

  const summaryItems = [
    {
      title: "Patients",
      value: summary?.patientsCount,
      description: "Total registered patients",
      icon: Users,
    },
    {
      title: "Analyses",
      value: summary?.analysesCount,
      description: "Total submitted scans",
      icon: FileImage,
    },
    {
      title: "Completed",
      value: summary?.completedAnalysesCount,
      description: "Finished AI analyses",
      icon: CheckCircle2,
    },
    {
      title: "Queued",
      value: summary?.queuedAnalysesCount,
      description: "Waiting for processing",
      icon: Clock3,
    },
    {
      title: "Failed",
      value: summary?.failedAnalysesCount,
      description: "Analyses requiring attention",
      icon: AlertTriangle,
    },
  ];

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Could not load dashboard summary</AlertTitle>
        <AlertDescription>{errorMessage}</AlertDescription>
      </Alert>
    );
  }

  return (
    <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
      {summaryItems.map((item) => {
        const Icon = item.icon;

        return (
          <Card key={item.title}>
            <CardHeader className="flex flex-row items-center justify-between gap-4 space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {item.title}
              </CardTitle>

              <Icon className="size-4 text-muted-foreground" />
            </CardHeader>

            <CardContent>
              {isLoading ? (
                <Skeleton className="h-9 w-16" />
              ) : (
                <div className="text-3xl font-bold">
                  {formatCount(item.value)}
                </div>
              )}

              <p className="mt-1 text-xs text-muted-foreground">
                {item.description}
              </p>
            </CardContent>
          </Card>
        );
      })}
    </section>
  );
}

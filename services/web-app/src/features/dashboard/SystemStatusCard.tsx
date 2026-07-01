"use client";

import { Activity, RefreshCw } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getSystemStatus } from "@/api/systemApi";
import { ApiClientError } from "@/lib/apiClient";
import type { SystemComponentStatus } from "@/types/systemStatus";

import { SystemHealthIndicator } from "@/components/status/SystemHealthIndicator";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

type ComponentItem = {
  key: string;
  label: string;
};

const componentItems: ComponentItem[] = [
  { key: "database", label: "Database" },
  { key: "rabbitmq", label: "RabbitMQ" },
  { key: "minio", label: "MinIO" },
  { key: "aiWorker", label: "AI Worker" },
];

function getStatusVariant(status?: string) {
  if (status === "UP") {
    return "default";
  }

  if (status === "DOWN") {
    return "destructive";
  }

  return "secondary";
}

function getComponentStatus(component?: SystemComponentStatus): string {
  return component?.status ?? "UNKNOWN";
}

export function SystemStatusCard() {
  const { data, isLoading, isError, error, isFetching, refetch } = useQuery({
    queryKey: ["system-status"],
    queryFn: getSystemStatus,
    refetchInterval: 10_000,
  });

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : "Could not load system status";

  const overallStatus = data?.status ?? (isLoading ? "LOADING" : "UNKNOWN");

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Activity className="size-5" />
              System status
            </CardTitle>
            <CardDescription>
              Health overview of MediScanFlow services.
            </CardDescription>
          </div>

          <Badge variant={getStatusVariant(data?.status)}>
            {overallStatus}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {isError && (
          <Alert variant="destructive">
            <AlertTitle>System status unavailable</AlertTitle>
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        <div className="space-y-3">
          {componentItems.map((item) => {
            const status = getComponentStatus(data?.components[item.key]);

            return (
              <div key={item.key} className="rounded-lg border px-3 py-2">
                <SystemHealthIndicator label={item.label} status={status} />
              </div>
            );
          })}
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          className="w-full"
          onClick={() => refetch()}
          disabled={isFetching}
        >
          <RefreshCw
            className={isFetching ? "size-4 animate-spin" : "size-4"}
          />
          Refresh status
        </Button>
      </CardContent>
    </Card>
  );
}

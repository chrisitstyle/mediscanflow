"use client";

import { Activity, RefreshCw } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getSystemStatus } from "@/api/systemApi";
import { SystemStatusBadge } from "@/components/status/SystemStatusBadge";
import { SystemHealthIndicator } from "@/components/status/SystemHealthIndicator";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";
import type { SystemComponentStatus } from "@/types/systemStatus";

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

function getComponentStatus(component?: SystemComponentStatus): string {
  return component?.status ?? "UNKNOWN";
}

function getStatusDotClass(status?: string) {
  if (status === "UP") {
    return "bg-emerald-500";
  }

  if (status === "DOWN") {
    return "bg-red-500";
  }

  if (status === "DEGRADED") {
    return "bg-amber-500";
  }

  return "bg-muted-foreground";
}

export function SystemStatusPopover() {
  const { data, isLoading, isError, error, isFetching, refetch } = useQuery({
    queryKey: queryKeys.system.status(),
    queryFn: getSystemStatus,
    refetchInterval: 10_000,
  });

  const overallStatus = data?.status ?? (isLoading ? "LOADING" : "UNKNOWN");

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : "Could not load system status";

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-2 rounded-full"
        >
          <span
            className={`size-3 rounded-full ${getStatusDotClass(data?.status)}`}
          />
          System status
        </Button>
      </PopoverTrigger>

      <PopoverContent align="end" className="w-96 p-4">
        <div className="space-y-4">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h3 className="font-semibold">System status</h3>

              <p className="mt-1 text-sm text-muted-foreground">
                Current health of MediScanFlow services.
              </p>
            </div>

            <SystemStatusBadge status={overallStatus} />
          </div>

          {isError && (
            <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {errorMessage}
            </div>
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
            className="w-full gap-2"
            onClick={() => void refetch()}
            disabled={isFetching}
          >
            <RefreshCw
              className={isFetching ? "size-4 animate-spin" : "size-4"}
            />
            Refresh status
          </Button>

          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Activity className="size-3.5" />
            Refreshes automatically every 10 seconds.
          </p>
        </div>
      </PopoverContent>
    </Popover>
  );
}

"use client";

import { Activity, Circle, RefreshCw } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getSystemStatus } from "@/api/systemApi";
import type { SystemComponentStatus } from "@/types/systemStatus";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

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

function getStatusDotClassName(status?: string) {
  if (status === "UP") {
    return "fill-green-500 text-green-500";
  }

  if (status === "DOWN") {
    return "fill-red-500 text-red-500";
  }

  return "fill-muted text-muted-foreground";
}

function getComponentStatus(component?: SystemComponentStatus) {
  return component?.status ?? "UNKNOWN";
}

export function SystemStatusPopover() {
  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: ["system-status"],
    queryFn: getSystemStatus,
    refetchInterval: 10_000,
  });

  const overallStatus = data?.status ?? (isLoading ? "LOADING" : "UNKNOWN");

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" type="button">
          <Activity className="size-4" />
          System status
          <span className="sr-only">Open system status details</span>
        </Button>
      </PopoverTrigger>

      <PopoverContent align="end" className="w-80">
        <div className="space-y-4">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-sm font-semibold">System status</h2>
              <p className="mt-1 text-xs text-muted-foreground">
                Current health of MediScanFlow services.
              </p>
            </div>

            <Badge variant={getStatusVariant(data?.status)}>
              {overallStatus}
            </Badge>
          </div>

          <div className="space-y-2">
            {componentItems.map((item) => {
              const status = getComponentStatus(data?.components[item.key]);

              return (
                <div
                  key={item.key}
                  className="flex items-center justify-between rounded-md border px-3 py-2"
                >
                  <div className="flex items-center gap-2">
                    <Circle
                      className={`size-3 ${getStatusDotClassName(status)}`}
                    />
                    <span className="text-sm font-medium">{item.label}</span>
                  </div>

                  <span className="text-xs text-muted-foreground"></span>
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
        </div>
      </PopoverContent>
    </Popover>
  );
}

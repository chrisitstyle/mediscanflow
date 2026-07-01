import { Circle } from "lucide-react";

import { cn } from "@/lib/utils";

type SystemHealthIndicatorProps = {
  label: string;
  status?: string;
  className?: string;
};

type SystemStatusDotProps = {
  status?: string;
  className?: string;
};

function getStatusDotClassName(status?: string) {
  if (status === "UP") {
    return "fill-green-500 text-green-500";
  }

  if (status === "DOWN") {
    return "fill-red-500 text-red-500";
  }

  if (status === "LOADING") {
    return "fill-muted text-muted-foreground animate-pulse";
  }

  return "fill-muted text-muted-foreground";
}

export function SystemStatusDot({ status, className }: SystemStatusDotProps) {
  return (
    <Circle
      className={cn("size-3", getStatusDotClassName(status), className)}
      aria-hidden="true"
    />
  );
}

export function SystemHealthIndicator({
  label,
  status = "UNKNOWN",
  className,
}: SystemHealthIndicatorProps) {
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <SystemStatusDot status={status} />
      <span className="text-sm font-medium">{label}</span>
      <span className="sr-only">{status}</span>
    </div>
  );
}

import { StatusBadge } from "@/components/status/StatusBadge";
import type { StatusTone } from "@/components/status/statusStyles";

type SystemStatusBadgeProps = {
  status: string;
};

const SYSTEM_STATUS_TONES: Record<string, StatusTone> = {
  UP: "success",
  DEGRADED: "warning",
  DOWN: "danger",
  UNKNOWN: "neutral",
  LOADING: "neutral",
};

export function SystemStatusBadge({ status }: SystemStatusBadgeProps) {
  return (
    <StatusBadge tone={SYSTEM_STATUS_TONES[status] ?? "neutral"}>
      {status}
    </StatusBadge>
  );
}

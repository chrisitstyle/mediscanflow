import type { AnalysisStatus } from "@/types/analysis";

import { StatusBadge } from "@/components/status/StatusBadge";
import type { StatusTone } from "@/components/status/statusStyles";

type AnalysisStatusBadgeProps = {
  status: AnalysisStatus;
};

const ANALYSIS_STATUS_TONES = {
  COMPLETED: "success",
  FAILED: "danger",
  UPLOADED: "info",
  PROCESSING: "warning",
  QUEUED: "neutral",
} satisfies Partial<Record<AnalysisStatus, StatusTone>>;

export function AnalysisStatusBadge({ status }: AnalysisStatusBadgeProps) {
  return (
    <StatusBadge tone={ANALYSIS_STATUS_TONES[status] ?? "neutral"}>
      {status}
    </StatusBadge>
  );
}

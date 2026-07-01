import type { AnalysisStatus } from "@/types/analysis";

import { Badge } from "@/components/ui/badge";

type AnalysisStatusBadgeProps = {
  status: AnalysisStatus;
};

function getAnalysisStatusVariant(
  status: AnalysisStatus,
): "default" | "secondary" | "destructive" | "outline" {
  if (status === "COMPLETED") {
    return "default";
  }

  if (status === "FAILED") {
    return "destructive";
  }

  if (status === "UPLOADED") {
    return "outline";
  }

  return "secondary";
}

export function AnalysisStatusBadge({ status }: AnalysisStatusBadgeProps) {
  return <Badge variant={getAnalysisStatusVariant(status)}>{status}</Badge>;
}

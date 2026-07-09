import {
  Activity,
  Archive,
  Download,
  RefreshCcw,
  RotateCcw,
  UploadCloud,
  UserCheck,
  UserCog,
  UserPlus,
  UserRoundPen,
  UserX,
} from "lucide-react";

import type { AuditEventType } from "@/types/audit";

type AuditEventIconProps = {
  type: AuditEventType;
  className?: string;
};

export function AuditEventIcon({
  type,
  className = "size-4 text-muted-foreground",
}: AuditEventIconProps) {
  switch (type) {
    case "PATIENT_CREATED":
      return <UserPlus className={className} />;
    case "PATIENT_PROFILE_UPDATED":
      return <UserRoundPen className={className} />;
    case "PATIENT_ARCHIVED":
      return <Archive className={className} />;
    case "PATIENT_RESTORED":
      return <RotateCcw className={className} />;
    case "ANALYSIS_UPLOADED":
      return <UploadCloud className={className} />;
    case "ANALYSIS_RETRIED":
      return <RefreshCcw className={className} />;
    case "REPORT_DOWNLOADED":
      return <Download className={className} />;
    case "USER_CREATED":
      return <UserCog className={className} />;
    case "USER_ENABLED":
      return <UserCheck className={className} />;
    case "USER_DISABLED":
      return <UserX className={className} />;
    default:
      return <Activity className={className} />;
  }
}

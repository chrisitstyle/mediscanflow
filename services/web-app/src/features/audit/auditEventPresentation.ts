import type { AuditEventType } from "@/types/audit";

export const AUDIT_EVENT_LABELS = {
  PATIENT_CREATED: "Patient created",
  PATIENT_PROFILE_UPDATED: "Patient updated",
  PATIENT_ARCHIVED: "Patient archived",
  PATIENT_RESTORED: "Patient restored",
  ANALYSIS_UPLOADED: "Analysis uploaded",
  ANALYSIS_RETRIED: "Analysis retried",
  REPORT_DOWNLOADED: "Report downloaded",
  USER_CREATED: "User created",
  USER_ENABLED: "User enabled",
  USER_DISABLED: "User disabled",
} satisfies Record<AuditEventType, string>;

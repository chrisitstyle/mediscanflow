export type AuditEventType =
  | "PATIENT_CREATED"
  | "PATIENT_PROFILE_UPDATED"
  | "PATIENT_ARCHIVED"
  | "PATIENT_RESTORED"
  | "ANALYSIS_UPLOADED"
  | "ANALYSIS_RETRIED"
  | "REPORT_DOWNLOADED";

export type AuditEvent = {
  id: string;
  type: AuditEventType;
  actorUserId: string | null;
  actorEmail: string | null;
  actorRole: string | null;
  patientId: string | null;
  analysisId: string | null;
  message: string;
  metadata: string | null;
  createdAt: string;
};

export type GetAuditEventsInput = {
  limit?: number;
};

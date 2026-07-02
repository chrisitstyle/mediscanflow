import { apiFetch } from "@/lib/apiClient";
import type { AuditEvent, GetAuditEventsInput } from "@/types/audit";

function buildAuditLimitQuery(input: GetAuditEventsInput = {}): string {
  const searchParams = new URLSearchParams();

  if (input.limit) {
    searchParams.set("limit", String(input.limit));
  }

  const query = searchParams.toString();

  return query ? `?${query}` : "";
}

export async function getRecentAuditEvents(
  input: GetAuditEventsInput = {},
): Promise<AuditEvent[]> {
  return apiFetch<AuditEvent[]>(
    `/audit-events/recent${buildAuditLimitQuery(input)}`,
  );
}

export async function getPatientAuditEvents(
  patientId: string,
  input: GetAuditEventsInput = {},
): Promise<AuditEvent[]> {
  return apiFetch<AuditEvent[]>(
    `/patients/${patientId}/audit-events${buildAuditLimitQuery(input)}`,
  );
}

export async function getAnalysisAuditEvents(
  analysisId: string,
  input: GetAuditEventsInput = {},
): Promise<AuditEvent[]> {
  return apiFetch<AuditEvent[]>(
    `/analyses/${analysisId}/audit-events${buildAuditLimitQuery(input)}`,
  );
}

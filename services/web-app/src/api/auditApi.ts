import { apiFetch } from "@/lib/apiClient";
import type {
  AuditEvent,
  AuditEventPage,
  GetAuditEventsInput,
  GetAuditEventsPageInput,
} from "@/types/audit";

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

export async function getAuditEventsPage(
  input: GetAuditEventsPageInput = {},
): Promise<AuditEventPage> {
  const searchParams = new URLSearchParams();

  searchParams.set("page", String(input.page ?? 0));
  searchParams.set("size", String(input.size ?? 50));

  return apiFetch<AuditEventPage>(`/audit-events?${searchParams.toString()}`);
}

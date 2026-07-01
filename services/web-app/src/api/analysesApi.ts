import { apiFetch } from "@/lib/apiClient";
import type { Analysis } from "@/types/analysis";

export function getPatientAnalyses(patientId: string): Promise<Analysis[]> {
  return apiFetch<Analysis[]>(`/patients/${patientId}/analyses`);
}

export function getAnalysis(analysisId: string): Promise<Analysis> {
  return apiFetch<Analysis>(`/analyses/${analysisId}`);
}

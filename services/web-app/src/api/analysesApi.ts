import { apiFetch } from "@/lib/apiClient";
import type { Analysis } from "@/types/analysis";
import type { AnalysisListItem } from "@/types/analysisListItem";
import type { RecentAnalysis } from "@/types/recentAnalysis";

export type UploadScanInput = {
  patientId: string;
  file: File;
  modelName?: string;
  modelVersion?: string;
};

export function getAnalyses(): Promise<AnalysisListItem[]> {
  return apiFetch<AnalysisListItem[]>("/analyses");
}

export function getPatientAnalyses(patientId: string): Promise<Analysis[]> {
  return apiFetch<Analysis[]>(`/patients/${patientId}/analyses`);
}

export function getAnalysis(analysisId: string): Promise<Analysis> {
  return apiFetch<Analysis>(`/analyses/${analysisId}`);
}

export function getRecentAnalyses(limit = 10): Promise<RecentAnalysis[]> {
  return apiFetch<RecentAnalysis[]>(`/analyses/recent?limit=${limit}`);
}

export function retryAnalysis(analysisId: string): Promise<Analysis> {
  return apiFetch<Analysis>(`/analyses/${analysisId}/retry`, {
    method: "POST",
  });
}

export function uploadScan(input: UploadScanInput): Promise<Analysis> {
  const formData = new FormData();

  formData.append("file", input.file);

  if (input.modelName) {
    formData.append("modelName", input.modelName);
  }

  if (input.modelVersion) {
    formData.append("modelVersion", input.modelVersion);
  }

  return apiFetch<Analysis>(`/patients/${input.patientId}/analyses`, {
    method: "POST",
    body: formData,
  });
}

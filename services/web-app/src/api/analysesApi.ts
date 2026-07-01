import { apiFetch } from "@/lib/apiClient";
import type { Analysis } from "@/types/analysis";

export type UploadScanInput = {
  patientId: string;
  file: File;
  modelName?: string;
  modelVersion?: string;
};

export function getPatientAnalyses(patientId: string): Promise<Analysis[]> {
  return apiFetch<Analysis[]>(`/patients/${patientId}/analyses`);
}

export function getAnalysis(analysisId: string): Promise<Analysis> {
  return apiFetch<Analysis>(`/analyses/${analysisId}`);
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

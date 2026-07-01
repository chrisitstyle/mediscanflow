import type { AnalysisStatus } from "@/types/analysis";

export type AnalysisListItem = {
  id: string;
  patientId: string;
  patientFullName: string;
  status: AnalysisStatus;
  originalFileName: string;
  modelName: string;
  modelVersion: string;
  fileSizeBytes: number;
  createdAt: string;
  completedAt: string | null;
};

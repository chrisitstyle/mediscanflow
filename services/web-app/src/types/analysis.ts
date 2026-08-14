export type AnalysisStatus =
  | "UPLOADED"
  | "QUEUED"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED";

export type AnalysisDetection = {
  label: string;
  confidence: number;
  x: number;
  y: number;
  width: number;
  height: number;
};

export type Analysis = {
  id: string;
  patientId: string;
  status: AnalysisStatus;
  originalFileName: string;
  objectKey: string;
  resultObjectKey: string | null;
  originalImageUrl: string | null;
  resultImageUrl: string | null;
  contentType: string;
  fileSizeBytes: number;
  modelName: string | null;
  modelVersion: string | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  detections: AnalysisDetection[];
};

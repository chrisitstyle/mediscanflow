export type DashboardSummary = {
  patientsCount: number;
  analysesCount: number;
  queuedAnalysesCount: number;
  completedAnalysesCount: number;
  failedAnalysesCount: number;
};

export type AnalysisStatus =
  | "UPLOADED"
  | "QUEUED"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED";

export type AnalysisStatusCount = {
  status: AnalysisStatus;
  count: number;
};

export type DailyAnalysisCount = {
  date: string;
  count: number;
};

export const queryKeys = {
  patients: {
    all: ["patients"] as const,
    list: (search = "") => ["patients", "list", search] as const,
    detail: (patientId: string) => ["patients", "detail", patientId] as const,
    analyses: (patientId: string) =>
      ["patients", "detail", patientId, "analyses"] as const,
  },

  analyses: {
    all: ["analyses"] as const,
    list: () => ["analyses", "list"] as const,
    detail: (analysisId: string) => ["analyses", "detail", analysisId] as const,
    recent: () => ["analyses", "recent"] as const,
  },

  dashboard: {
    summary: () => ["dashboard", "summary"] as const,
  },

  system: {
    status: () => ["system", "status"] as const,
  },
};

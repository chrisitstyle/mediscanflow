export const queryKeys = {
  patients: {
    all: ["patients"] as const,
    list: (search = "", includeArchived = false) =>
      ["patients", "list", search, includeArchived] as const,
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

  audit: {
    recent: (limit = 20) => ["audit", "recent", limit] as const,
    page: (page = 0, size = 50) => ["audit", "page", page, size] as const,
    patient: (patientId: string, limit = 20) =>
      ["audit", "patient", patientId, limit] as const,
    analysis: (analysisId: string, limit = 20) =>
      ["audit", "analysis", analysisId, limit] as const,
  },

  dashboard: {
    summary: () => ["dashboard", "summary"] as const,
    analysisStatusBreakdown: () =>
      ["dashboard", "analysis-status-breakdown"] as const,
    analysesOverTime: (days = 14) =>
      ["dashboard", "analyses-over-time", days] as const,
  },

  system: {
    status: () => ["system", "status"] as const,
  },

  users: {
    all: ["users"] as const,
    list: () => ["users", "list"] as const,
    detail: (userId: string) => ["users", "detail", userId] as const,
  },

  auth: {
    me: () => ["auth", "me"] as const,
  },
};

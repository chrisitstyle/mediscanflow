package com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto;

public record DashboardSummaryDTO(
        long patientsCount,
        long analysesCount,
        long queuedAnalysesCount,
        long processingAnalysesCount,
        long completedAnalysesCount,
        long failedAnalysesCount
) {
}

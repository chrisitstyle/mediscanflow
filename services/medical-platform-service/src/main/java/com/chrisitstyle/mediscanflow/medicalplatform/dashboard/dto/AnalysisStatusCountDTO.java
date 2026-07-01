package com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;

public record AnalysisStatusCountDTO(
        AnalysisStatus status,
        long count
) {
}

package com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto;

import java.util.UUID;

public record AnalysisDetectionDTO(
        UUID id,
        String label,
        double confidence,
        double x,
        double y,
        double width,
        double height
) {
}

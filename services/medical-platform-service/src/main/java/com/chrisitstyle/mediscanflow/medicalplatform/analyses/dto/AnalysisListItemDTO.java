package com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public record AnalysisListItemDTO(
        UUID id,
        UUID patientId,
        String patientFullName,
        AnalysisStatus status,
        String originalFileName,
        String modelName,
        String modelVersion,
        long fileSizeBytes,
        Instant createdAt,
        Instant completedAt
) {
}

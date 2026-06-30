package com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponseDTO(
        UUID id,
        UUID patientId,
        AnalysisStatus status,
        String originalFileName,
        String objectKey,
        String contentType,
        long fileSizeBytes,
        String modelName,
        String modelVersion,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        List<AnalysisDetectionDTO> detections
) {
}

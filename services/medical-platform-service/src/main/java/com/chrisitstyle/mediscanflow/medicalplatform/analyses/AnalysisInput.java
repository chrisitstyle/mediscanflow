package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

public record AnalysisInput(
        String originalFileName,
        String objectKey,
        String contentType,
        long fileSizeBytes,
        String modelName,
        String modelVersion
) {
}

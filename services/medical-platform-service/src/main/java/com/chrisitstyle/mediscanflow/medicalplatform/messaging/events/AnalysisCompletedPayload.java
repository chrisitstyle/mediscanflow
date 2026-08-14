package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.util.List;
import java.util.UUID;

public record AnalysisCompletedPayload(
        UUID analysisId,
        UUID attemptId,
        String modelName,
        String modelVersion,
        String resultObjectKey,
        List<AnalysisDetectionPayload> detections
) {
}

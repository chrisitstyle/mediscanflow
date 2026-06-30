package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.util.List;
import java.util.UUID;

public record AnalysisCompletedPayload(
        UUID analysisId,
        String modelName,
        String modelVersion,
        List<AnalysisDetectionPayload> detections
) {
}

package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.util.UUID;

public record AnalysisRequestedPayload(
        UUID analysisId,
        UUID patientId,
        String objectKey,
        String modelName,
        String modelVersion
) {
}

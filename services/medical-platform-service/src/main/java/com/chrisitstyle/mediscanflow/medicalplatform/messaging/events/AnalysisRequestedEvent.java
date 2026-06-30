package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record AnalysisRequestedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        AnalysisRequestedPayload payload
) {

    public static AnalysisRequestedEvent create(
            UUID analysisId,
            UUID patientId,
            String objectKey,
            String modelName,
            String modelVersion
    ) {
        return new AnalysisRequestedEvent(
                UUID.randomUUID(),
                "AnalysisRequested",
                1,
                Instant.now(),
                UUID.randomUUID(),
                new AnalysisRequestedPayload(
                        analysisId,
                        patientId,
                        objectKey,
                        modelName,
                        modelVersion
                )
        );
    }
}

package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record AnalysisProcessingStartedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        AnalysisProcessingStartedPayload payload
) {

    public static final String TYPE = "AnalysisProcessingStarted";
    public static final int VERSION = 1;
}

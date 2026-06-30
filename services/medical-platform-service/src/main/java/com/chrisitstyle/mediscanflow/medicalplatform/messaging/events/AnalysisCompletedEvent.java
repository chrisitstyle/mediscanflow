package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record AnalysisCompletedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        AnalysisCompletedPayload payload
) {
}
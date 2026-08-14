package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AnalysisRequestedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        AnalysisRequestedPayload payload
) {

    public static final String TYPE = "AnalysisRequested";
    public static final int VERSION = 3;

    public static AnalysisRequestedEvent create(
            UUID analysisId,
            UUID patientId,
            String objectKey,
            UUID attemptId) {

        Objects.requireNonNull(attemptId,"attemptId must not be null");

        return new AnalysisRequestedEvent(
                UUID.randomUUID(),
                TYPE,
                VERSION,
                Instant.now(),
                UUID.randomUUID(),
                new AnalysisRequestedPayload(
                        analysisId,
                        patientId,
                        objectKey,
                        attemptId));
    }
}

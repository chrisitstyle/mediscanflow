package com.chrisitstyle.mediscanflow.medicalplatform.audit.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditEventDTO(
        UUID id,
        AuditEventType type,
        String actorUserId,
        String actorEmail,
        String actorRole,
        UUID patientId,
        UUID analysisId,
        String message,
        String metadata,
        Instant createdAt
) {
}

package com.chrisitstyle.mediscanflow.medicalplatform.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditEventType type;

    @Column(length = 128)
    private String actorUserId;

    @Column(length = 255)
    private String actorEmail;

    @Column(length = 64)
    private String actorRole;

    @Column
    private UUID patientId;

    @Column
    private UUID analysisId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }


}

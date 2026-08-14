package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisDetectionPayload;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String objectKey;

    private String contentType;

    @Column(nullable = false)
    private long fileSizeBytes;

    private UUID processingAttemptId;

    private String modelName;

    private String modelVersion;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    @Column(length = 500)
    private String resultObjectKey;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AnalysisDetection> detections = new ArrayList<>();

    private Analysis(
            UUID id,
            Patient patient,
            AnalysisStatus status,
            AnalysisInput input,
            UUID processingAttemptId,
            Instant createdAt) {
        this.id = id;
        this.patient = patient;
        this.status = status;
        this.originalFileName = input.originalFileName();
        this.objectKey = input.objectKey();
        this.contentType = input.contentType();
        this.fileSizeBytes = input.fileSizeBytes();
        this.processingAttemptId = processingAttemptId;
        this.createdAt = createdAt;
    }

    static Analysis uploaded(
            UUID id,
            Patient patient,
            AnalysisInput input) {
        return new Analysis(
                id,
                patient,
                AnalysisStatus.UPLOADED,
                input,
                null,
                Instant.now());
    }

    static Analysis queued(
            UUID id,
            Patient patient,
            AnalysisInput input) {
        return new Analysis(
                id,
                patient,
                AnalysisStatus.QUEUED,
                input,
                UUID.randomUUID(),
                Instant.now());
    }

    public void startProcessing() {
        if (this.status == AnalysisStatus.PROCESSING
                || this.status == AnalysisStatus.COMPLETED
                || this.status == AnalysisStatus.FAILED) {
            return;
        }

        if (this.status != AnalysisStatus.QUEUED) {
            throw new InvalidAnalysisStateException(
                    "Only queued analyses can start processing."
            );
        }

        this.status = AnalysisStatus.PROCESSING;
    }

    public void complete(
            String modelName,
            String modelVersion,
            String resultObjectKey,
            List<AnalysisDetectionPayload> detectionPayloads) {
        if (this.status == AnalysisStatus.COMPLETED) {
            return;
        }

        validateCanBeCompleted();

        this.status = AnalysisStatus.COMPLETED;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.resultObjectKey = resultObjectKey;
        this.completedAt = Instant.now();
        this.errorMessage = null;

        this.detections.clear();

        detectionPayloads.forEach(detectionPayload -> this.detections.add(
                AnalysisDetection.create(
                        this,
                        detectionPayload.label(),
                        detectionPayload.confidence(),
                        detectionPayload.x(),
                        detectionPayload.y(),
                        detectionPayload.width(),
                        detectionPayload.height())));
    }

    public void fail(
            String modelName,
            String modelVersion,
            String errorMessage) {
        if (this.status == AnalysisStatus.FAILED
                || this.status == AnalysisStatus.COMPLETED) {
            return;
        }

        validateCanBeFailed();

        this.status = AnalysisStatus.FAILED;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.errorMessage = errorMessage;
        this.resultObjectKey = null;
        this.completedAt = Instant.now();
        this.detections.clear();
    }

    public void retry() {
        validateCanBeRetried();

        this.status = AnalysisStatus.QUEUED;
        this.processingAttemptId = UUID.randomUUID();
        this.modelName = null;
        this.modelVersion = null;
        this.errorMessage = null;
        this.completedAt = null;
        this.resultObjectKey = null;
        this.detections.clear();
    }

    public boolean isCurrentProcessingAttempt(UUID attemptId) {
        return this.processingAttemptId != null
                && this.processingAttemptId.equals(attemptId);
    }

    private void validateCanBeRetried() {
        if (this.status != AnalysisStatus.FAILED) {
            throw new InvalidAnalysisStateException("Only failed analyses can be retried.");
        }
    }

    private void validateCanBeCompleted() {
        if (isInactive()) {
            throw new InvalidAnalysisStateException("Only queued or processing analyses can be completed.");
        }
    }

    private void validateCanBeFailed() {
        if (isInactive()) {
            throw new InvalidAnalysisStateException("Only queued or processing analyses can fail.");
        }
    }

    private boolean isInactive() {
        return this.status != AnalysisStatus.QUEUED
                && this.status != AnalysisStatus.PROCESSING;
    }
}
package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

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

    private String modelName;

    private String modelVersion;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisDetection> detections = new ArrayList<>();

    private Analysis(
            UUID id,
            Patient patient,
            AnalysisStatus status,
            String originalFileName,
            String objectKey,
            String contentType,
            long fileSizeBytes,
            String modelName,
            String modelVersion,
            Instant createdAt
    ) {
        this.id = id;
        this.patient = patient;
        this.status = status;
        this.originalFileName = originalFileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
    }

    public static Analysis uploaded(
            UUID id,
            Patient patient,
            String originalFileName,
            String objectKey,
            String contentType,
            long fileSizeBytes,
            String modelName,
            String modelVersion
    ) {
        return new Analysis(
                id,
                patient,
                AnalysisStatus.UPLOADED,
                originalFileName,
                objectKey,
                contentType,
                fileSizeBytes,
                modelName,
                modelVersion,
                Instant.now()
        );
    }

    public static Analysis queued(
            UUID id,
            Patient patient,
            String originalFileName,
            String objectKey,
            String contentType,
            long fileSizeBytes,
            String modelName,
            String modelVersion
    ) {
        return new Analysis(
                id,
                patient,
                AnalysisStatus.QUEUED,
                originalFileName,
                objectKey,
                contentType,
                fileSizeBytes,
                modelName,
                modelVersion,
                Instant.now()
        );
    }

    public void complete(
            String modelName,
            String modelVersion,
            List<AnalysisDetectionPayload> detectionPayloads
    ) {
        if (this.status == AnalysisStatus.COMPLETED) {
            return;
        }

        this.status = AnalysisStatus.COMPLETED;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.completedAt = Instant.now();
        this.errorMessage = null;

        this.detections.clear();

        detectionPayloads.forEach(detectionPayload ->
                this.detections.add(
                        AnalysisDetection.create(
                                this,
                                detectionPayload.label(),
                                detectionPayload.confidence(),
                                detectionPayload.x(),
                                detectionPayload.y(),
                                detectionPayload.width(),
                                detectionPayload.height()
                        )
                )
        );
    }
}

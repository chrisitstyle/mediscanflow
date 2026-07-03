package com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public interface AnalysisSummaryProjection {

    UUID getId();
    PatientProjection getPatient();
    AnalysisStatus getStatus();
    String getOriginalFileName();
    String getModelName();
    String getModelVersion();
    Long getFileSizeBytes();
    Instant getCreatedAt();
    Instant getCompletedAt();

    interface PatientProjection {

        UUID getId();
        String getFirstName();
        String getLastName();
    }

}

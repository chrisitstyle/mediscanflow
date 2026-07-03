package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Factory class for creating reusable analysis test fixtures.
 * <p>
 * Keeps analysis-related test data in one place, so service tests can focus on
 * behavior instead of object setup.
 */
public final class AnalysisTestEntity {

    public static final UUID ANALYSIS_ID =
            UUID.fromString("4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24");

    public static final UUID PATIENT_ID =
            UUID.fromString("9efdb5f0-733e-4f59-8a78-6240e43237c7");

    public static final String PATIENT_FULL_NAME = "John Doe";
    public static final String ORIGINAL_FILE_NAME = "brain-scan.jpg";
    public static final String OBJECT_KEY =
            "analyses/4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24/brain-scan.jpg";
    public static final String ORIGINAL_IMAGE_URL =
            "http://localhost:9000/medical-scans/brain-scan.jpg";
    public static final String CONTENT_TYPE = "image/jpeg";
    public static final long FILE_SIZE_BYTES = 30310L;
    public static final String MODEL_NAME = "yolo-brain-tumor-detector";
    public static final String MODEL_VERSION = "yolov8n";

    public static final Instant CREATED_AT = Instant.parse("2026-07-01T10:00:00Z");
    public static final Instant COMPLETED_AT = Instant.parse("2026-07-01T10:00:08Z");

    private static final String FAILURE_MESSAGE = "Simulated inference failure";

    private AnalysisTestEntity() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a list item DTO representing a completed analysis.
     *
     * @return analysis list item DTO with fixed test values
     */
    public static AnalysisListItemDTO analysisListItem() {
        return new AnalysisListItemDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                PATIENT_FULL_NAME,
                AnalysisStatus.COMPLETED,
                ORIGINAL_FILE_NAME,
                MODEL_NAME,
                MODEL_VERSION,
                FILE_SIZE_BYTES,
                CREATED_AT,
                COMPLETED_AT
        );
    }

    /**
     * Creates a response DTO for an analysis with the given status.
     *
     * @param status status to set on the response DTO
     * @return analysis response DTO with fixed test values
     */
    public static AnalysisResponseDTO analysisResponseDTO(AnalysisStatus status) {
        return new AnalysisResponseDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                status,
                ORIGINAL_FILE_NAME,
                OBJECT_KEY,
                ORIGINAL_IMAGE_URL,
                null,
                null,
                CONTENT_TYPE,
                FILE_SIZE_BYTES,
                MODEL_NAME,
                MODEL_VERSION,
                null,
                CREATED_AT,
                null,
                List.of()
        );
    }

    /**
     * Creates a failed analysis entity.
     *
     * @return failed analysis with fixed test values
     */
    public static Analysis failedAnalysis() {
        Analysis analysis = queuedAnalysis();

        analysis.fail(
                MODEL_NAME,
                MODEL_VERSION,
                FAILURE_MESSAGE
        );

        return analysis;
    }

    /**
     * Creates a queued analysis entity.
     *
     * @return queued analysis with fixed test values
     */
    public static Analysis queuedAnalysis() {
        AnalysisInput analysisInput = new AnalysisInput(
                ORIGINAL_FILE_NAME,
                OBJECT_KEY,
                CONTENT_TYPE,
                FILE_SIZE_BYTES,
                MODEL_NAME,
                MODEL_VERSION
        );

        return Analysis.queued(
                ANALYSIS_ID,
                patient(),
                analysisInput
        );
    }

    private static Patient patient() {
        Patient patient = mock(Patient.class);

        when(patient.getId()).thenReturn(PATIENT_ID);

        return patient;
    }
}
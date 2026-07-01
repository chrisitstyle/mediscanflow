package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class AnalysisRetryTest {

    @Test
    void retryMovesFailedAnalysisBackToQueuedAndClearsFailureState() {
        Analysis analysis = Analysis.queued(
                UUID.fromString("4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"),
                mock(Patient.class),
                "brain-scan.jpg",
                "analyses/4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24/brain-scan.jpg",
                "image/jpeg",
                30310L,
                "yolo-brain-tumor-detector",
                "yolov8n"
        );

        analysis.fail(
                "yolo-brain-tumor-detector",
                "yolov8n",
                "Simulated inference failure"
        );

        analysis.retry();

        assertEquals(AnalysisStatus.QUEUED, analysis.getStatus());
        assertNull(analysis.getErrorMessage());
        assertNull(analysis.getCompletedAt());
        assertNull(analysis.getResultObjectKey());
        assertEquals(0, analysis.getDetections().size());
    }
}

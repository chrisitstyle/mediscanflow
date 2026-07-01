package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerRetryTest {

    private static final UUID ANALYSIS_ID =
            UUID.fromString("4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24");

    private static final UUID PATIENT_ID =
            UUID.fromString("9efdb5f0-733e-4f59-8a78-6240e43237c7");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @Test
    void retryAnalysisReturnsQueuedAnalysis() throws Exception {
        when(analysisService.retryAnalysis(ANALYSIS_ID))
                .thenReturn(retriedAnalysisResponse());

        mockMvc.perform(post("/api/analyses/{id}/retry", ANALYSIS_ID).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.originalFileName").value("brain-scan.jpg"))
                .andExpect(jsonPath("$.objectKey").value("analyses/%s/brain-scan.jpg".formatted(ANALYSIS_ID)))
                .andExpect(jsonPath("$.resultObjectKey").doesNotExist())
                .andExpect(jsonPath("$.resultImageUrl").doesNotExist())
                .andExpect(jsonPath("$.errorMessage").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andExpect(jsonPath("$.detections").isArray())
                .andExpect(jsonPath("$.detections").isEmpty());

        verify(analysisService).retryAnalysis(ANALYSIS_ID);
    }

    @Test
    void retryAnalysisReturnsConflictWhenAnalysisIsNotFailed() throws Exception {
        when(analysisService.retryAnalysis(ANALYSIS_ID))
                .thenThrow(new InvalidAnalysisStateException(
                        "Only failed analyses can be retried."
                ));

        mockMvc.perform(post("/api/analyses/{id}/retry", ANALYSIS_ID).contextPath("/api"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only failed analyses can be retried."))
                .andExpect(jsonPath("$.path").value("/api/analyses/%s/retry".formatted(ANALYSIS_ID)));
    }

    @Test
    void retryAnalysisReturnsNotFoundWhenAnalysisDoesNotExist() throws Exception {
        when(analysisService.retryAnalysis(ANALYSIS_ID))
                .thenThrow(new ResourceNotFoundException(
                        "Analysis not found with id: " + ANALYSIS_ID
                ));

        mockMvc.perform(post("/api/analyses/{id}/retry", ANALYSIS_ID).contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Analysis not found with id: " + ANALYSIS_ID))
                .andExpect(jsonPath("$.path").value("/api/analyses/%s/retry".formatted(ANALYSIS_ID)));
    }

    private static AnalysisResponseDTO retriedAnalysisResponse() {
        return new AnalysisResponseDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                AnalysisStatus.QUEUED,
                "brain-scan.jpg",
                "analyses/%s/brain-scan.jpg".formatted(ANALYSIS_ID),
                "http://localhost:9000/medical-scans/brain-scan.jpg",
                null,
                null,
                "image/jpeg",
                30310L,
                "yolo-brain-tumor-detector",
                "yolov8n",
                null,
                Instant.parse("2026-07-01T10:00:00Z"),
                null,
                List.of()
        );
    }
}

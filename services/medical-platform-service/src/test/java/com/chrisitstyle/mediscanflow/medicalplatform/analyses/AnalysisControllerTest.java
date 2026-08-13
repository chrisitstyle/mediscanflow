package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportService;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    private static final UUID ANALYSIS_ID = UUID.fromString("4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24");

    private static final UUID PATIENT_ID = UUID.fromString("9efdb5f0-733e-4f59-8a78-6240e43237c7");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisCreationService analysisCreationService;
    @MockitoBean
    private AnalysisQueryService analysisQueryService;
    @MockitoBean
    private AnalysisLifecycleService analysisLifecycleService;
    @MockitoBean
    private AnalysisReportService analysisReportService;

    @Test
    void findAllAnalysesReturnsAnalysisListItems() throws Exception {
        when(analysisQueryService.findAllAnalyses())
                .thenReturn(List.of(analysisListItem()));

        mockMvc.perform(get("/api/analyses").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$[0].patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$[0].patientFullName").value(PATIENT_FULL_NAME))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].originalFileName").value(ORIGINAL_FILE_NAME))
                .andExpect(jsonPath("$[0].modelName").value(MODEL_NAME))
                .andExpect(jsonPath("$[0].modelVersion").value(MODEL_VERSION))
                .andExpect(jsonPath("$[0].fileSizeBytes").value(FILE_SIZE_BYTES))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].completedAt").exists());

        verify(analysisQueryService).findAllAnalyses();
    }

    @Test
    void getRecentAnalysesReturnsLatestAnalysesWithDefaultLimit() throws Exception {
        when(analysisQueryService.findRecentAnalyses(10))
                .thenReturn(List.of(recentAnalysis()));

        mockMvc.perform(get("/api/analyses/recent").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$[0].patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$[0].patientFullName").value(PATIENT_FULL_NAME))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].originalFileName").value(ORIGINAL_FILE_NAME))
                .andExpect(jsonPath("$[0].modelName").value(MODEL_NAME))
                .andExpect(jsonPath("$[0].modelVersion").value(MODEL_VERSION))
                .andExpect(jsonPath("$[0].fileSizeBytes").value(FILE_SIZE_BYTES))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].completedAt").exists());

        verify(analysisQueryService).findRecentAnalyses(10);
    }

    @Test
    void getRecentAnalysesPassesCustomLimitToService() throws Exception {
        when(analysisQueryService.findRecentAnalyses(5))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analyses/recent?limit=5").contextPath("/api"))
                .andExpect(status().isOk());

        verify(analysisQueryService).findRecentAnalyses(5);
    }

    @Test
    void retryAnalysisReturnsQueuedAnalysis() throws Exception {
        when(analysisLifecycleService.retryAnalysis(ANALYSIS_ID))
                .thenReturn(retriedAnalysisResponse());

        mockMvc.perform(post("/api/analyses/{id}/retry", ANALYSIS_ID).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANALYSIS_ID.toString()))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.originalFileName").value(ORIGINAL_FILE_NAME))
                .andExpect(jsonPath("$.objectKey")
                        .value("analyses/%s/%s".formatted(ANALYSIS_ID, ORIGINAL_FILE_NAME)))
                .andExpect(jsonPath("$.originalImageUrl")
                        .value("http://localhost:9000/medical-scans/brain-scan.jpg"))
                .andExpect(jsonPath("$.resultObjectKey").isEmpty())
                .andExpect(jsonPath("$.resultImageUrl").isEmpty())
                .andExpect(jsonPath("$.errorMessage").isEmpty())
                .andExpect(jsonPath("$.completedAt").isEmpty())
                .andExpect(jsonPath("$.detections").isArray())
                .andExpect(jsonPath("$.detections").isEmpty());

        verify(analysisLifecycleService).retryAnalysis(ANALYSIS_ID);
    }

    @Test
    void retryAnalysisReturnsConflictWhenAnalysisIsNotFailed() throws Exception {
        when(analysisLifecycleService.retryAnalysis(ANALYSIS_ID))
                .thenThrow(new InvalidAnalysisStateException(
                        "Only failed analyses can be retried."
                ));

        mockMvc.perform(post("/api/analyses/{id}/retry", ANALYSIS_ID).contextPath("/api"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only failed analyses can be retried."))
                .andExpect(jsonPath("$.path")
                        .value("/api/analyses/%s/retry".formatted(ANALYSIS_ID)));

        verify(analysisLifecycleService).retryAnalysis(ANALYSIS_ID);
    }

    @Test
    void retryAnalysisReturnsNotFoundWhenAnalysisDoesNotExist() throws Exception {
        when(analysisLifecycleService.retryAnalysis(ANALYSIS_ID))
                .thenThrow(new ResourceNotFoundException(
                        "Analysis not found with id: " + ANALYSIS_ID));

        mockMvc.perform(post("/api/analyses/{id}/retry", ANALYSIS_ID).contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Analysis not found with id: " + ANALYSIS_ID))
                .andExpect(jsonPath("$.path")
                        .value("/api/analyses/%s/retry".formatted(ANALYSIS_ID)));

        verify(analysisLifecycleService).retryAnalysis(ANALYSIS_ID);
    }

    private static AnalysisListItemDTO analysisListItem() {
        return new AnalysisListItemDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                PATIENT_FULL_NAME,
                AnalysisStatus.COMPLETED,
                ORIGINAL_FILE_NAME,
                MODEL_NAME,
                MODEL_VERSION,
                FILE_SIZE_BYTES,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:00:08Z"));
    }

    private static RecentAnalysisDTO recentAnalysis() {
        return new RecentAnalysisDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                PATIENT_FULL_NAME,
                AnalysisStatus.COMPLETED,
                ORIGINAL_FILE_NAME,
                MODEL_NAME,
                MODEL_VERSION,
                FILE_SIZE_BYTES,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:00:08Z"));
    }

    private static AnalysisResponseDTO retriedAnalysisResponse() {
        return new AnalysisResponseDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                AnalysisStatus.QUEUED,
                ORIGINAL_FILE_NAME,
                "analyses/%s/%s".formatted(ANALYSIS_ID, ORIGINAL_FILE_NAME),
                "http://localhost:9000/medical-scans/brain-scan.jpg",
                null,
                null,
                "image/jpeg",
                FILE_SIZE_BYTES,
                MODEL_NAME,
                MODEL_VERSION,
                null,
                Instant.parse("2026-07-01T10:00:00Z"),
                null,
                List.of());
    }
}
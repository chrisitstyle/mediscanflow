package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerRecentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @Test
    void getRecentAnalysesReturnsLatestAnalysesWithDefaultLimit() throws Exception {
        UUID analysisId = UUID.fromString("0d0d58bb-fc23-467f-b052-5f126bfa9411");
        UUID patientId = UUID.fromString("d1dc0244-af11-464c-baf1-6bd4a9f05cf1");

        RecentAnalysisDTO recentAnalysis = new RecentAnalysisDTO(
                analysisId,
                patientId,
                "John Doe",
                AnalysisStatus.COMPLETED,
                "brain-scan.jpg",
                "yolo-brain-tumor-detector",
                "yolov8n",
                30310L,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:00:08Z")
        );

        when(analysisService.findRecentAnalyses(10))
                .thenReturn(List.of(recentAnalysis));

        mockMvc.perform(get("/api/analyses/recent").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(analysisId.toString()))
                .andExpect(jsonPath("$[0].patientId").value(patientId.toString()))
                .andExpect(jsonPath("$[0].patientFullName").value("John Doe"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].originalFileName").value("brain-scan.jpg"))
                .andExpect(jsonPath("$[0].modelName").value("yolo-brain-tumor-detector"))
                .andExpect(jsonPath("$[0].modelVersion").value("yolov8n"))
                .andExpect(jsonPath("$[0].fileSizeBytes").value(30310))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].completedAt").exists());

        verify(analysisService).findRecentAnalyses(10);
    }

    @Test
    void getRecentAnalysesPassesCustomLimitToService() throws Exception {
        when(analysisService.findRecentAnalyses(5))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analyses/recent?limit=5").contextPath("/api"))
                .andExpect(status().isOk());

        verify(analysisService).findRecentAnalyses(5);
    }
}

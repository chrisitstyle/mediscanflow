package com.chrisitstyle.mediscanflow.medicalplatform.dashboard;

import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DashboardSummaryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getSummaryReturnsDashboardCounts() throws Exception {
        when(dashboardService.getSummary())
                .thenReturn(new DashboardSummaryDTO(
                        3,
                        12,
                        2,
                        9,
                        1
                ));

        mockMvc.perform(get("/api/dashboard/summary").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientsCount").value(3))
                .andExpect(jsonPath("$.analysesCount").value(12))
                .andExpect(jsonPath("$.queuedAnalysesCount").value(2))
                .andExpect(jsonPath("$.completedAnalysesCount").value(9))
                .andExpect(jsonPath("$.failedAnalysesCount").value(1));
    }
}

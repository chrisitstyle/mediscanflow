package com.chrisitstyle.mediscanflow.medicalplatform.dashboard;

import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.AnalysisStatusCountDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DailyAnalysisCountDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DashboardSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    DashboardSummaryDTO getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/analysis-status-breakdown")
    public List<AnalysisStatusCountDTO> getAnalysisStatusBreakdown() {
        return dashboardService.getAnalysisStatusBreakdown();
    }

    @GetMapping("/analyses-over-time")
    public List<DailyAnalysisCountDTO> getAnalysesOverTime(
            @RequestParam(defaultValue = "14") int days
    ) {
        return dashboardService.getAnalysesOverTime(days);
    }
}

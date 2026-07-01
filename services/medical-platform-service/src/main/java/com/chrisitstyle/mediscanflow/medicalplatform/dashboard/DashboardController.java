package com.chrisitstyle.mediscanflow.medicalplatform.dashboard;

import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DashboardSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    DashboardSummaryDTO getSummary() {
        return dashboardService.getSummary();
    }
}

package com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto;

import java.time.LocalDate;

public record DailyAnalysisCountDTO(
        LocalDate date,
        long count
) {
}

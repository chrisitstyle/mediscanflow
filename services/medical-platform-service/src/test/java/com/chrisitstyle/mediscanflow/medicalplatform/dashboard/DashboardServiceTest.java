package com.chrisitstyle.mediscanflow.medicalplatform.dashboard;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DashboardSummaryDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryCountsPatientsAndAnalysesByStatus() {
        when(patientRepository.count()).thenReturn(3L);
        when(analysisRepository.count()).thenReturn(12L);
        when(analysisRepository.countByStatus(AnalysisStatus.QUEUED)).thenReturn(2L);
        when(analysisRepository.countByStatus(AnalysisStatus.COMPLETED)).thenReturn(9L);
        when(analysisRepository.countByStatus(AnalysisStatus.FAILED)).thenReturn(1L);

        DashboardSummaryDTO summary = dashboardService.getSummary();

        assertAll(
                () -> assertEquals(3L, summary.patientsCount()),
                () -> assertEquals(12L, summary.analysesCount()),
                () -> assertEquals(2L, summary.queuedAnalysesCount()),
                () -> assertEquals(9L, summary.completedAnalysesCount()),
                () -> assertEquals(1L, summary.failedAnalysesCount())
        );
    }
}

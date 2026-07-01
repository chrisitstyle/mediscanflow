package com.chrisitstyle.mediscanflow.medicalplatform.dashboard;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DashboardSummaryDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DashboardService {

    private final PatientRepository patientRepository;
    private final AnalysisRepository analysisRepository;

    DashboardSummaryDTO getSummary() {
        long patientsCount = patientRepository.count();
        long analysesCount = analysisRepository.count();
        long queuedAnalysesCount = analysisRepository.countByStatus(AnalysisStatus.QUEUED);
        long completedAnalysesCount = analysisRepository.countByStatus(AnalysisStatus.COMPLETED);
        long failedAnalysesCount = analysisRepository.countByStatus(AnalysisStatus.FAILED);

        return new DashboardSummaryDTO(
                patientsCount,
                analysesCount,
                queuedAnalysesCount,
                completedAnalysesCount,
                failedAnalysesCount
        );
    }
}

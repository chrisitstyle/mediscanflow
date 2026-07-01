package com.chrisitstyle.mediscanflow.medicalplatform.dashboard;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.AnalysisStatusCountDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DailyAnalysisCountDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.DashboardSummaryDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<AnalysisStatusCountDTO> getAnalysisStatusBreakdown() {
        Map<AnalysisStatus, Long> counts = new EnumMap<>(AnalysisStatus.class);

        Arrays.stream(AnalysisStatus.values())
                .forEach(status -> counts.put(status, 0L));

        analysisRepository.countAnalysesByStatus()
                .forEach(statusCount -> counts.put(statusCount.status(), statusCount.count()));

        return Arrays.stream(AnalysisStatus.values())
                .map(status -> new AnalysisStatusCountDTO(status, counts.get(status)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DailyAnalysisCountDTO> getAnalysesOverTime(int days) {
        int safeDays = Math.clamp(days, 1, 30);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = today.minusDays(safeDays - 1L);

        Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();

        for (int index = 0; index < safeDays; index++) {
            countsByDate.put(startDate.plusDays(index), 0L);
        }

        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Analysis> analyses = analysisRepository.findByCreatedAtGreaterThanEqual(startInstant);

        Map<LocalDate, Long> actualCounts = analyses.stream()
                .collect(Collectors.groupingBy(
                        analysis -> analysis.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.counting()
                ));

        actualCounts.forEach((date, count) -> {
            if (countsByDate.containsKey(date)) {
                countsByDate.put(date, count);
            }
        });

        return countsByDate.entrySet()
                .stream()
                .map(entry -> new DailyAnalysisCountDTO(entry.getKey(), entry.getValue()))
                .toList();
    }
}

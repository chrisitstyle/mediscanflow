package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Provides read-only operations for retrieving analyses.
 */
@Service
@RequiredArgsConstructor
public class AnalysisQueryService {

    private static final String ANALYSIS_NOT_FOUND_MSG = "Analysis not found";

    private final AnalysisRepository analysisRepository;
    private final AnalysisMapper analysisMapper;

    @Transactional(readOnly = true)
    public AnalysisResponseDTO findById(UUID id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ANALYSIS_NOT_FOUND_MSG));

        return analysisMapper.toResponseDTO(analysis);
    }

    @Transactional(readOnly = true)
    public List<AnalysisListItemDTO> findAllAnalyses() {
        return analysisRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(analysisMapper::toListItemDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponseDTO> findByPatientId(UUID patientId) {
        return analysisRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(analysisMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentAnalysisDTO> findRecentAnalyses(int limit) {
        int safeLimit = Math.clamp(limit, 1, 20);
        Pageable pageable = PageRequest.of(0, safeLimit);

        return analysisRepository.findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(analysisMapper::toRecentAnalysisDTO)
                .toList();
    }
}

package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection.AnalysisSummaryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.ANALYSIS_ID;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.COMPLETED_AT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.CREATED_AT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.FILE_SIZE_BYTES;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.MODEL_NAME;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.MODEL_VERSION;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.ORIGINAL_FILE_NAME;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.PATIENT_FULL_NAME;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.PATIENT_ID;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.analysisListItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisQueryServiceTest {

    private AnalysisRepository analysisRepository;
    private AnalysisMapper analysisMapper;
    private AnalysisQueryService analysisQueryService;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        analysisMapper = mock(AnalysisMapper.class);

        analysisQueryService = new AnalysisQueryService(
                analysisRepository,
                analysisMapper);
    }

    @Test
    void findAllAnalysesReturnsAnalysisListItemsFromRepository() {
        AnalysisSummaryProjection projection = mock(AnalysisSummaryProjection.class);
        AnalysisListItemDTO listItem = analysisListItem();

        when(analysisRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(projection));

        when(analysisMapper.toListItemDTO(projection))
                .thenReturn(listItem);

        List<AnalysisListItemDTO> analyses = analysisQueryService.findAllAnalyses();

        assertEquals(1, analyses.size());

        AnalysisListItemDTO analysis = analyses.getFirst();

        assertEquals(ANALYSIS_ID, analysis.id());
        assertEquals(PATIENT_ID, analysis.patientId());
        assertEquals(PATIENT_FULL_NAME, analysis.patientFullName());
        assertEquals(AnalysisStatus.COMPLETED, analysis.status());
        assertEquals(ORIGINAL_FILE_NAME, analysis.originalFileName());
        assertEquals(MODEL_NAME, analysis.modelName());
        assertEquals(MODEL_VERSION, analysis.modelVersion());
        assertEquals(FILE_SIZE_BYTES, analysis.fileSizeBytes());
        assertEquals(CREATED_AT, analysis.createdAt());
        assertEquals(COMPLETED_AT, analysis.completedAt());

        verify(analysisRepository).findAllByOrderByCreatedAtDesc();
        verify(analysisMapper).toListItemDTO(projection);
    }

    @ParameterizedTest
    @CsvSource({
            "5, 5",
            "0, 1",
            "100, 20"
    })
    void findRecentAnalysesUsesSafeLimit(
            int requestedLimit,
            int expectedPageSize) {
        when(analysisRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

        analysisQueryService.findRecentAnalyses(requestedLimit);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(analysisRepository).findAllByOrderByCreatedAtDesc(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(expectedPageSize, pageable.getPageSize());
    }
}

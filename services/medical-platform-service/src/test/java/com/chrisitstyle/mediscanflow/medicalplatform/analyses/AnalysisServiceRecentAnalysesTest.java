package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceRecentAnalysesTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private AnalysisService analysisService;

    @ParameterizedTest
    @CsvSource({
            "5, 5",
            "0, 1",
            "100, 20"
    })
    void findRecentAnalysesUsesSafeLimit(int requestedLimit, int expectedPageSize) {
        when(analysisRepository.findRecentAnalyses(any(Pageable.class)))
                .thenReturn(List.of());

        analysisService.findRecentAnalyses(requestedLimit);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(analysisRepository).findRecentAnalyses(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(expectedPageSize, pageable.getPageSize());
    }
}
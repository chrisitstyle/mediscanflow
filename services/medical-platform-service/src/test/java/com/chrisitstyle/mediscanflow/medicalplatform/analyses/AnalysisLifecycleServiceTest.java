package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.ANALYSIS_ID;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.PATIENT_ID;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.analysisResponseDTO;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.failedAnalysis;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.queuedAnalysis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisLifecycleServiceTest {

    private AnalysisRepository analysisRepository;
    private AuditEventService auditEventService;
    private AnalysisMapper analysisMapper;
    private OutboxEventService outboxEventService;
    private AnalysisLifecycleService analysisLifecycleService;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        auditEventService = mock(AuditEventService.class);
        analysisMapper = mock(AnalysisMapper.class);
        outboxEventService = mock(OutboxEventService.class);

        analysisLifecycleService = new AnalysisLifecycleService(
                analysisRepository,
                auditEventService,
                analysisMapper,
                outboxEventService);
    }

    @Test
    void retryMovesFailedAnalysisBackToQueued() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(analysisMapper.toResponseDTO(failedAnalysis))
                .thenReturn(analysisResponseDTO(AnalysisStatus.QUEUED));

        AnalysisResponseDTO response =
                analysisLifecycleService.retryAnalysis(ANALYSIS_ID);

        assertEquals(AnalysisStatus.QUEUED, failedAnalysis.getStatus());
        assertNull(failedAnalysis.getErrorMessage());
        assertNull(failedAnalysis.getCompletedAt());
        assertNull(failedAnalysis.getResultObjectKey());
        assertEquals(0, failedAnalysis.getDetections().size());

        assertEquals(ANALYSIS_ID, response.id());
        assertEquals(PATIENT_ID, response.patientId());
        assertEquals(AnalysisStatus.QUEUED, response.status());
        assertNull(response.errorMessage());
        assertNull(response.completedAt());
        assertNull(response.resultObjectKey());

        verify(analysisMapper).toResponseDTO(failedAnalysis);
        verify(outboxEventService)
                .saveAnalysisRequestedEvent(failedAnalysis);

        verify(auditEventService).recordEvent(
                AuditEventType.ANALYSIS_RETRIED,
                PATIENT_ID,
                ANALYSIS_ID,
                "Analysis " + ANALYSIS_ID + " was retried.");
    }

    @Test
    void retryStoresRequestedEventInOutbox() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(analysisMapper.toResponseDTO(failedAnalysis))
                .thenReturn(analysisResponseDTO(AnalysisStatus.QUEUED));

        analysisLifecycleService.retryAnalysis(ANALYSIS_ID);

        verify(outboxEventService)
                .saveAnalysisRequestedEvent(failedAnalysis);
    }

    @Test
    void retryThrowsWhenAnalysisIsNotFailed() {
        Analysis queuedAnalysis = queuedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(queuedAnalysis));

        assertThrows(InvalidAnalysisStateException.class,
                () -> analysisLifecycleService.retryAnalysis(ANALYSIS_ID));

        verify(outboxEventService, never()).saveAnalysisRequestedEvent(any(Analysis.class));

        verify(auditEventService, never()).recordEvent(
                any(),
                any(),
                any(),
                anyString()
        );

        verify(analysisMapper, never()).toResponseDTO(any(Analysis.class));
    }

    @Test
    void retryThrowsWhenAnalysisDoesNotExist() {
        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> analysisLifecycleService.retryAnalysis(ANALYSIS_ID)
        );

        verify(outboxEventService, never())
                .saveAnalysisRequestedEvent(any(Analysis.class));

        verify(auditEventService, never()).recordEvent(
                any(),
                any(),
                any(),
                anyString());

        verify(analysisMapper, never()).toResponseDTO(any(Analysis.class));
    }
}

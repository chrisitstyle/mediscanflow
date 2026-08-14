package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisDetectionPayload;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AnalysisLifecycleServiceTest {

    private AnalysisRepository analysisRepository;
    private AuditEventService auditEventService;
    private AnalysisMapper analysisMapper;
    private OutboxEventService outboxEventService;
    private AnalysisLifecycleService analysisLifecycleService;

    private static final String MODEL_NAME = "yolo-brain-tumor-detector";
    private static final String MODEL_VERSION = "yolov8n";
    private static final String RESULT_OBJECT_KEY = "analyses/%s/result.jpg".formatted(ANALYSIS_ID);
    private static final String FAILURE_MESSAGE = "Simulated inference failure";

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
    void startProcessingProcessesCurrentAttempt() {
        Analysis analysis = queuedAnalysis();
        UUID attemptId = analysis.getProcessingAttemptId();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(analysis));

        analysisLifecycleService.startProcessing(
                ANALYSIS_ID,
                attemptId);

        assertEquals(
                AnalysisStatus.PROCESSING,
                analysis.getStatus());
        assertEquals(
                attemptId,
                analysis.getProcessingAttemptId());
    }

    @Test
    void startProcessingIgnoresStaleAttempt() {
        Analysis analysis = queuedAnalysis();

        UUID currentAttemptId = analysis.getProcessingAttemptId();
        UUID staleAttemptId = UUID.randomUUID();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(analysis));

        analysisLifecycleService.startProcessing(
                ANALYSIS_ID,
                staleAttemptId);

        assertEquals(
                AnalysisStatus.QUEUED,
                analysis.getStatus());
        assertEquals(
                currentAttemptId,
                analysis.getProcessingAttemptId());
    }

    @Test
    void retryMovesFailedAnalysisBackToQueued() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(analysisMapper.toResponseDTO(failedAnalysis))
                .thenReturn(analysisResponseDTO(AnalysisStatus.QUEUED));

        AnalysisResponseDTO response = analysisLifecycleService.retryAnalysis(ANALYSIS_ID);

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

    @Test
    void completeProcessesCurrentAttempt() {
        Analysis analysis = queuedAnalysis();
        UUID attemptId = analysis.getProcessingAttemptId();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(analysis));

        analysisLifecycleService.complete(
                ANALYSIS_ID,
                attemptId,
                MODEL_NAME,
                MODEL_VERSION,
                RESULT_OBJECT_KEY,
                detections()
        );

        assertEquals(AnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(MODEL_NAME, analysis.getModelName());
        assertEquals(MODEL_VERSION, analysis.getModelVersion());
        assertEquals(RESULT_OBJECT_KEY, analysis.getResultObjectKey());
        assertNotNull(analysis.getCompletedAt());
        assertEquals(1, analysis.getDetections().size());
    }

    @Test
    void completeIgnoresStaleAttempt() {
        Analysis analysis = queuedAnalysis();

        UUID currentAttemptId = analysis.getProcessingAttemptId();
        UUID staleAttemptId = UUID.randomUUID();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(analysis));

        analysisLifecycleService.complete(
                ANALYSIS_ID,
                staleAttemptId,
                MODEL_NAME,
                MODEL_VERSION,
                RESULT_OBJECT_KEY,
                detections()
        );

        assertEquals(AnalysisStatus.QUEUED, analysis.getStatus());
        assertEquals(currentAttemptId, analysis.getProcessingAttemptId());
        assertNull(analysis.getModelName());
        assertNull(analysis.getModelVersion());
        assertNull(analysis.getResultObjectKey());
        assertNull(analysis.getCompletedAt());
        assertTrue(analysis.getDetections().isEmpty());
    }

    @Test
    void failProcessesCurrentAttempt() {
        Analysis analysis = queuedAnalysis();
        UUID attemptId = analysis.getProcessingAttemptId();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(analysis));

        analysisLifecycleService.fail(
                ANALYSIS_ID,
                attemptId,
                MODEL_NAME,
                MODEL_VERSION,
                FAILURE_MESSAGE
        );

        assertEquals(AnalysisStatus.FAILED, analysis.getStatus());
        assertEquals(MODEL_NAME, analysis.getModelName());
        assertEquals(MODEL_VERSION, analysis.getModelVersion());
        assertEquals(FAILURE_MESSAGE, analysis.getErrorMessage());
        assertNotNull(analysis.getCompletedAt());
    }

    @Test
    void failIgnoresStaleAttempt() {
        Analysis analysis = queuedAnalysis();

        UUID currentAttemptId = analysis.getProcessingAttemptId();
        UUID staleAttemptId = UUID.randomUUID();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(analysis));

        analysisLifecycleService.fail(
                ANALYSIS_ID,
                staleAttemptId,
                MODEL_NAME,
                MODEL_VERSION,
                FAILURE_MESSAGE
        );

        assertEquals(AnalysisStatus.QUEUED, analysis.getStatus());
        assertEquals(currentAttemptId, analysis.getProcessingAttemptId());
        assertNull(analysis.getModelName());
        assertNull(analysis.getModelVersion());
        assertNull(analysis.getErrorMessage());
        assertNull(analysis.getCompletedAt());
    }

    private static List<AnalysisDetectionPayload> detections() {
        return List.of(
                new AnalysisDetectionPayload(
                        "glioma",
                        0.92,
                        10,
                        20,
                        100,
                        80
                )
        );
    }
}

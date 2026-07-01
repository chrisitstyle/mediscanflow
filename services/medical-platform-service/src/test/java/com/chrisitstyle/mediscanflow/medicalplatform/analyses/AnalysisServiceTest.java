package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.validation.FileUploadValidator;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.AnalysisEventPublisher;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private static final UUID ANALYSIS_ID =
            UUID.fromString("4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24");

    private static final UUID PATIENT_ID =
            UUID.fromString("9efdb5f0-733e-4f59-8a78-6240e43237c7");

    private static final String OBJECT_KEY =
            "analyses/4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24/brain-scan.jpg";

    private AnalysisRepository analysisRepository;
    private FileStorageService fileStorageService;
    private AnalysisEventPublisher analysisEventPublisher;
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        PatientRepository patientRepository = mock(PatientRepository.class);
        fileStorageService = mock(FileStorageService.class);
        FileUploadValidator fileUploadValidator = mock(FileUploadValidator.class);
        analysisEventPublisher = mock(AnalysisEventPublisher.class);

        analysisService = new AnalysisService(
                analysisRepository,
                patientRepository,
                fileStorageService,
                fileUploadValidator,
                analysisEventPublisher
        );

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void findAllAnalysesReturnsAnalysisListItemsFromRepository() {
        when(analysisRepository.findAllAnalysisListItems())
                .thenReturn(List.of(analysisListItem()));

        List<AnalysisListItemDTO> analyses = analysisService.findAllAnalyses();

        assertEquals(1, analyses.size());

        AnalysisListItemDTO analysis = analyses.getFirst();

        assertEquals(ANALYSIS_ID, analysis.id());
        assertEquals(PATIENT_ID, analysis.patientId());
        assertEquals("John Doe", analysis.patientFullName());
        assertEquals(AnalysisStatus.COMPLETED, analysis.status());
        assertEquals("brain-scan.jpg", analysis.originalFileName());
        assertEquals("yolo-brain-tumor-detector", analysis.modelName());
        assertEquals("yolov8n", analysis.modelVersion());
        assertEquals(30310L, analysis.fileSizeBytes());
        assertEquals(Instant.parse("2026-07-01T10:00:00Z"), analysis.createdAt());
        assertEquals(Instant.parse("2026-07-01T10:00:08Z"), analysis.completedAt());

        verify(analysisRepository).findAllAnalysisListItems();
    }

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

    @Test
    void retryAnalysisMovesFailedAnalysisBackToQueued() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(fileStorageService.generatePresignedUrl(OBJECT_KEY))
                .thenReturn("http://localhost:9000/medical-scans/brain-scan.jpg");

        AnalysisResponseDTO response = analysisService.retryAnalysis(ANALYSIS_ID);

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
    }

    @Test
    void retryAnalysisPublishesRequestedEventAfterCommit() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(fileStorageService.generatePresignedUrl(OBJECT_KEY))
                .thenReturn("http://localhost:9000/medical-scans/brain-scan.jpg");

        analysisService.retryAnalysis(ANALYSIS_ID);

        verify(analysisEventPublisher, never())
                .publishAnalysisRequested(any(AnalysisRequestedEvent.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(analysisEventPublisher)
                .publishAnalysisRequested(any(AnalysisRequestedEvent.class));
    }

    @Test
    void retryAnalysisThrowsWhenAnalysisIsNotFailed() {
        Analysis queuedAnalysis = queuedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(queuedAnalysis));

        assertThrows(
                InvalidAnalysisStateException.class,
                () -> analysisService.retryAnalysis(ANALYSIS_ID)
        );

        verify(analysisEventPublisher, never())
                .publishAnalysisRequested(any(AnalysisRequestedEvent.class));
    }

    @Test
    void retryAnalysisThrowsWhenAnalysisDoesNotExist() {
        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> analysisService.retryAnalysis(ANALYSIS_ID)
        );

        verify(analysisEventPublisher, never())
                .publishAnalysisRequested(any(AnalysisRequestedEvent.class));
    }

    private static AnalysisListItemDTO analysisListItem() {
        return new AnalysisListItemDTO(
                ANALYSIS_ID,
                PATIENT_ID,
                "John Doe",
                AnalysisStatus.COMPLETED,
                "brain-scan.jpg",
                "yolo-brain-tumor-detector",
                "yolov8n",
                30310L,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:00:08Z")
        );
    }

    private static Analysis failedAnalysis() {
        Analysis analysis = queuedAnalysis();

        analysis.fail(
                "yolo-brain-tumor-detector",
                "yolov8n",
                "Simulated inference failure"
        );

        return analysis;
    }

    private static Analysis queuedAnalysis() {
        Patient patient = mock(Patient.class);

        when(patient.getId()).thenReturn(PATIENT_ID);

        return Analysis.queued(
                ANALYSIS_ID,
                patient,
                "brain-scan.jpg",
                OBJECT_KEY,
                "image/jpeg",
                30310L,
                "yolo-brain-tumor-detector",
                "yolov8n"
        );
    }
}
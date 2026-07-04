package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection.AnalysisSummaryProjection;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidPatientStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.validation.FileUploadValidator;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox.OutboxEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.UploadedFileCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.ANALYSIS_ID;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.COMPLETED_AT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.CONTENT_TYPE;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.CREATED_AT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.FILE_SIZE_BYTES;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.MODEL_NAME;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.MODEL_VERSION;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.OBJECT_KEY;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.ORIGINAL_FILE_NAME;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.PATIENT_FULL_NAME;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.PATIENT_ID;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.analysisListItem;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.analysisResponseDTO;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.failedAnalysis;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.queuedAnalysis;
import static com.chrisitstyle.mediscanflow.medicalplatform.testentities.PatientTestEntity.patient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private AnalysisRepository analysisRepository;
    private PatientRepository patientRepository;
    private FileStorageService fileStorageService;
    private FileUploadValidator fileUploadValidator;
    private AuditEventService auditEventService;
    private AnalysisMapper analysisMapper;
    private AnalysisObjectKeyFactory analysisObjectKeyFactory;
    private OutboxEventService outboxEventService;
    private UploadedFileCleanupService uploadedFileCleanupService;
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        patientRepository = mock(PatientRepository.class);
        fileStorageService = mock(FileStorageService.class);
        fileUploadValidator = mock(FileUploadValidator.class);
        auditEventService = mock(AuditEventService.class);
        analysisMapper = mock(AnalysisMapper.class);
        analysisObjectKeyFactory = mock(AnalysisObjectKeyFactory.class);
        outboxEventService = mock(OutboxEventService.class);
        uploadedFileCleanupService = mock(UploadedFileCleanupService.class);

        analysisService = new AnalysisService(
                analysisRepository,
                patientRepository,
                fileStorageService,
                fileUploadValidator,
                auditEventService,
                analysisMapper,
                analysisObjectKeyFactory,
                outboxEventService,
                uploadedFileCleanupService
        );
    }

    @Test
    void createUploadsFileSavesAnalysisRecordsAuditAndStoresOutboxEvent() {
        Patient patient = patient();
        MultipartFile file = multipartFile();

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Optional.of(patient));

        when(analysisObjectKeyFactory.create(any(UUID.class), eq(ORIGINAL_FILE_NAME)))
                .thenReturn(OBJECT_KEY);

        when(analysisRepository.save(any(Analysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(analysisMapper.toResponseDTO(any(Analysis.class)))
                .thenReturn(analysisResponseDTO(AnalysisStatus.QUEUED));

        AnalysisResponseDTO response = analysisService.create(
                PATIENT_ID,
                file,
                MODEL_NAME,
                MODEL_VERSION
        );

        assertEquals(ANALYSIS_ID, response.id());
        assertEquals(PATIENT_ID, response.patientId());
        assertEquals(AnalysisStatus.QUEUED, response.status());

        verify(fileUploadValidator).validateImageFile(file);
        verify(patientRepository).findById(PATIENT_ID);
        verify(analysisObjectKeyFactory).create(any(UUID.class), eq(ORIGINAL_FILE_NAME));
        verify(fileStorageService).upload(OBJECT_KEY, file);
        verify(uploadedFileCleanupService).deleteOnRollback(OBJECT_KEY);
        verify(analysisRepository).save(any(Analysis.class));
        verify(outboxEventService).saveAnalysisRequestedEvent(any(Analysis.class));
        verify(analysisMapper).toResponseDTO(any(Analysis.class));

        verify(auditEventService).recordEvent(
                eq(AuditEventType.ANALYSIS_UPLOADED),
                eq(patient.getId()),
                any(UUID.class),
                eq("Scan " + ORIGINAL_FILE_NAME + " was uploaded for analysis.")
        );
    }

    @Test
    void createDeletesUploadedFileWhenAnalysisCreationFailsWithoutActiveTransaction() {
        Patient patient = patient();
        MultipartFile file = multipartFile();
        RuntimeException repositoryException = new IllegalStateException("Database unavailable");

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Optional.of(patient));

        when(analysisObjectKeyFactory.create(any(UUID.class), eq(ORIGINAL_FILE_NAME)))
                .thenReturn(OBJECT_KEY);

        when(analysisRepository.save(any(Analysis.class)))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> analysisService.create(
                        PATIENT_ID,
                        file,
                        MODEL_NAME,
                        MODEL_VERSION
                )
        );

        assertEquals(repositoryException, exception);

        verify(fileStorageService).upload(OBJECT_KEY, file);
        verify(uploadedFileCleanupService).deleteOnRollback(OBJECT_KEY);
        verify(uploadedFileCleanupService).deleteIfNoActiveTransaction(OBJECT_KEY);
        verify(outboxEventService, never()).saveAnalysisRequestedEvent(any(Analysis.class));
        verify(analysisMapper, never()).toResponseDTO(any(Analysis.class));
    }

    @Test
    void createThrowsWhenPatientDoesNotExist() {
        MultipartFile file = multipartFile();

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> analysisService.create(
                        PATIENT_ID,
                        file,
                        MODEL_NAME,
                        MODEL_VERSION
                )
        );

        verify(fileUploadValidator).validateImageFile(file);
        verify(fileStorageService, never()).upload(anyString(), any(MultipartFile.class));
        verify(analysisRepository, never()).save(any(Analysis.class));
        verify(outboxEventService, never()).saveAnalysisRequestedEvent(any(Analysis.class));
    }

    @Test
    void createThrowsWhenPatientIsArchived() {
        Patient patient = patient();
        patient.archive();

        MultipartFile file = multipartFile();

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Optional.of(patient));

        assertThrows(
                InvalidPatientStateException.class,
                () -> analysisService.create(
                        PATIENT_ID,
                        file,
                        MODEL_NAME,
                        MODEL_VERSION
                )
        );

        verify(fileUploadValidator).validateImageFile(file);
        verify(fileStorageService, never()).upload(anyString(), any(MultipartFile.class));
        verify(analysisRepository, never()).save(any(Analysis.class));
        verify(outboxEventService, never()).saveAnalysisRequestedEvent(any(Analysis.class));
    }

    @Test
    void findAllAnalysesReturnsAnalysisListItemsFromRepository() {
        AnalysisSummaryProjection projection = mock(AnalysisSummaryProjection.class);
        AnalysisListItemDTO listItem = analysisListItem();

        when(analysisRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(projection));

        when(analysisMapper.toListItemDTO(projection))
                .thenReturn(listItem);

        List<AnalysisListItemDTO> analyses = analysisService.findAllAnalyses();

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
    void findRecentAnalysesUsesSafeLimit(int requestedLimit, int expectedPageSize) {
        when(analysisRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

        analysisService.findRecentAnalyses(requestedLimit);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(analysisRepository).findAllByOrderByCreatedAtDesc(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(expectedPageSize, pageable.getPageSize());
    }

    @Test
    void retryAnalysisMovesFailedAnalysisBackToQueued() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(analysisMapper.toResponseDTO(failedAnalysis))
                .thenReturn(analysisResponseDTO(AnalysisStatus.QUEUED));

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

        verify(analysisMapper).toResponseDTO(failedAnalysis);
        verify(outboxEventService).saveAnalysisRequestedEvent(failedAnalysis);

        verify(auditEventService).recordEvent(
                AuditEventType.ANALYSIS_RETRIED,
                PATIENT_ID,
                ANALYSIS_ID,
                "Analysis " + ANALYSIS_ID + " was retried."
        );
    }

    @Test
    void retryAnalysisStoresRequestedEventInOutbox() {
        Analysis failedAnalysis = failedAnalysis();

        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.of(failedAnalysis));

        when(analysisMapper.toResponseDTO(failedAnalysis))
                .thenReturn(analysisResponseDTO(AnalysisStatus.QUEUED));

        analysisService.retryAnalysis(ANALYSIS_ID);

        verify(outboxEventService).saveAnalysisRequestedEvent(failedAnalysis);
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
    void retryAnalysisThrowsWhenAnalysisDoesNotExist() {
        when(analysisRepository.findById(ANALYSIS_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> analysisService.retryAnalysis(ANALYSIS_ID)
        );

        verify(outboxEventService, never()).saveAnalysisRequestedEvent(any(Analysis.class));

        verify(auditEventService, never()).recordEvent(
                any(),
                any(),
                any(),
                anyString()
        );

        verify(analysisMapper, never()).toResponseDTO(any(Analysis.class));
    }

    private static MultipartFile multipartFile() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn(ORIGINAL_FILE_NAME);
        when(file.getContentType()).thenReturn(CONTENT_TYPE);
        when(file.getSize()).thenReturn(FILE_SIZE_BYTES);

        return file;
    }
}
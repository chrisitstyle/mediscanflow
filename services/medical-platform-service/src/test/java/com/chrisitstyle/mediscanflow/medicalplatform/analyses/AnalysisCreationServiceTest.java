package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.*;
import static com.chrisitstyle.mediscanflow.medicalplatform.testentities.PatientTestEntity.patient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AnalysisCreationServiceTest {

    private AnalysisRepository analysisRepository;
    private PatientRepository patientRepository;
    private FileStorageService fileStorageService;
    private FileUploadValidator fileUploadValidator;
    private AuditEventService auditEventService;
    private AnalysisMapper analysisMapper;
    private AnalysisObjectKeyFactory analysisObjectKeyFactory;
    private OutboxEventService outboxEventService;
    private UploadedFileCleanupService uploadedFileCleanupService;
    private AnalysisCreationService analysisCreationService;

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

        analysisCreationService = new AnalysisCreationService(
                analysisRepository,
                patientRepository,
                fileStorageService,
                fileUploadValidator,
                auditEventService,
                analysisMapper,
                analysisObjectKeyFactory,
                outboxEventService,
                uploadedFileCleanupService);
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

        AnalysisResponseDTO response = analysisCreationService.create(
                PATIENT_ID,
                file,
                MODEL_NAME,
                MODEL_VERSION);

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
                eq("Scan " + ORIGINAL_FILE_NAME + " was uploaded for analysis."));
    }

    @Test
    void createDeletesUploadedFileWhenAnalysisCreationFailsWithoutActiveTransaction() {
        Patient patient = patient();
        MultipartFile file = multipartFile();
        RuntimeException repositoryException =
                new IllegalStateException("Database unavailable");

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Optional.of(patient));

        when(analysisObjectKeyFactory.create(any(UUID.class), eq(ORIGINAL_FILE_NAME)))
                .thenReturn(OBJECT_KEY);

        when(analysisRepository.save(any(Analysis.class)))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> analysisCreationService.create(
                        PATIENT_ID,
                        file,
                        MODEL_NAME,
                        MODEL_VERSION));

        assertEquals(repositoryException, exception);

        verify(fileStorageService).upload(OBJECT_KEY, file);
        verify(uploadedFileCleanupService).deleteOnRollback(OBJECT_KEY);
        verify(uploadedFileCleanupService).deleteIfNoActiveTransaction(OBJECT_KEY);
        verify(outboxEventService, never())
                .saveAnalysisRequestedEvent(any(Analysis.class));
        verify(analysisMapper, never())
                .toResponseDTO(any(Analysis.class));
    }

    @Test
    void createThrowsWhenPatientDoesNotExist() {
        MultipartFile file = multipartFile();

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> analysisCreationService.create(
                        PATIENT_ID,
                        file,
                        MODEL_NAME,
                        MODEL_VERSION));

        verify(fileUploadValidator).validateImageFile(file);
        verify(fileStorageService, never())
                .upload(anyString(), any(MultipartFile.class));
        verify(analysisRepository, never())
                .save(any(Analysis.class));
        verify(outboxEventService, never())
                .saveAnalysisRequestedEvent(any(Analysis.class));
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
                () -> analysisCreationService.create(
                        PATIENT_ID,
                        file,
                        MODEL_NAME,
                        MODEL_VERSION
                )
        );

        verify(fileUploadValidator).validateImageFile(file);
        verify(fileStorageService, never())
                .upload(anyString(), any(MultipartFile.class));
        verify(analysisRepository, never())
                .save(any(Analysis.class));
        verify(outboxEventService, never())
                .saveAnalysisRequestedEvent(any(Analysis.class));
    }

    private static MultipartFile multipartFile() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn(ORIGINAL_FILE_NAME);
        when(file.getContentType()).thenReturn(CONTENT_TYPE);
        when(file.getSize()).thenReturn(FILE_SIZE_BYTES);

        return file;
    }
}

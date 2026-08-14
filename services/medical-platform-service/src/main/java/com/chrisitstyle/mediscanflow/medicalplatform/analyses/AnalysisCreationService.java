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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Handles creation of analyses, including input file storage
 * and initialization of the analysis workflow.
 */
@Service
@RequiredArgsConstructor
public class AnalysisCreationService {

    private static final String PATIENT_NOT_FOUND_MSG = "Patient not found";

    private final AnalysisRepository analysisRepository;
    private final PatientRepository patientRepository;
    private final FileStorageService fileStorageService;
    private final FileUploadValidator fileUploadValidator;
    private final AuditEventService auditEventService;
    private final AnalysisMapper analysisMapper;
    private final AnalysisObjectKeyFactory analysisObjectKeyFactory;
    private final OutboxEventService outboxEventService;
    private final UploadedFileCleanupService uploadedFileCleanupService;

    @Transactional
    public AnalysisResponseDTO create(
            UUID patientId,
            MultipartFile file) {
        fileUploadValidator.validateImageFile(file);

        Patient patient = findPatientOrThrow(patientId);
        validatePatientCanUploadScans(patient);

        UUID analysisId = UUID.randomUUID();
        String objectKey = analysisObjectKeyFactory.create(
                analysisId,
                file.getOriginalFilename());

        uploadInputFile(objectKey, file);

        try {
            Analysis analysis = createQueuedAnalysis(
                    analysisId,
                    patient,
                    file,
                    objectKey);

            Analysis savedAnalysis = saveAnalysis(analysis);

            return analysisMapper.toResponseDTO(savedAnalysis);
        } catch (RuntimeException exception) {
            uploadedFileCleanupService.deleteIfNoActiveTransaction(objectKey);
            throw exception;
        }
    }

    private Patient findPatientOrThrow(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_MSG));
    }

    private void validatePatientCanUploadScans(Patient patient) {
        if (patient.isArchived()) {
            throw new InvalidPatientStateException(
                    "Cannot upload scans for archived patient.");
        }
    }

    private void uploadInputFile(String objectKey, MultipartFile file) {
        fileStorageService.upload(objectKey, file);
        uploadedFileCleanupService.deleteOnRollback(objectKey);
    }

    private Analysis createQueuedAnalysis(
            UUID analysisId,
            Patient patient,
            MultipartFile file,
            String objectKey) {
        AnalysisInput analysisInput = new AnalysisInput(
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize());

        return Analysis.queued(
                analysisId,
                patient,
                analysisInput
        );
    }

    private Analysis saveAnalysis(Analysis analysis) {
        Analysis savedAnalysis = analysisRepository.save(analysis);

        recordAnalysisUploadedAudit(savedAnalysis);
        outboxEventService.saveAnalysisRequestedEvent(savedAnalysis);

        return savedAnalysis;
    }

    private void recordAnalysisUploadedAudit(Analysis analysis) {
        auditEventService.recordEvent(
                AuditEventType.ANALYSIS_UPLOADED,
                analysis.getPatient().getId(),
                analysis.getId(),
                "Scan %s was uploaded for analysis."
                        .formatted(analysis.getOriginalFileName())
        );
    }
}

package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidPatientStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.validation.FileUploadValidator;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisDetectionPayload;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox.OutboxEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.UploadedFileCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final String ANALYSIS_NOT_FOUND_MSG = "Analysis not found";
    private static final String ANALYSIS_NOT_FOUND_WITH_ID_MSG = "Analysis not found with id: ";
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
            MultipartFile file,
            String modelName,
            String modelVersion
    ) {
        fileUploadValidator.validateImageFile(file);

        Patient patient = findPatientOrThrow(patientId);
        validatePatientCanUploadScans(patient);

        UUID analysisId = UUID.randomUUID();
        String objectKey = createObjectKey(analysisId, file);

        uploadInputFile(objectKey, file);

        try {
            Analysis analysis = createQueuedAnalysis(
                    analysisId,
                    patient,
                    file,
                    objectKey,
                    modelName,
                    modelVersion
            );

            Analysis savedAnalysis = saveAnalysisWithAuditAndOutbox(analysis);

            return analysisMapper.toResponseDTO(savedAnalysis);
        } catch (RuntimeException exception) {
            uploadedFileCleanupService.deleteIfNoActiveTransaction(objectKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResponseDTO findById(UUID id) {
        Analysis analysis = findAnalysisOrThrow(id);

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

    @Transactional
    public AnalysisResponseDTO retryAnalysis(UUID analysisId) {
        Analysis analysis = findAnalysisForRetryOrThrow(analysisId);

        validateAnalysisCanBeRetried(analysis);

        analysis.retry();

        recordAnalysisRetriedAudit(analysis);
        outboxEventService.saveAnalysisRequestedEvent(analysis);

        return analysisMapper.toResponseDTO(analysis);
    }

    @Transactional
    public void complete(
            UUID analysisId,
            String modelName,
            String modelVersion,
            String resultObjectKey,
            List<AnalysisDetectionPayload> detections
    ) {
        Analysis analysis = findAnalysisOrThrow(analysisId);

        analysis.complete(modelName, modelVersion, resultObjectKey, detections);
    }

    @Transactional
    public void fail(
            UUID analysisId,
            String modelName,
            String modelVersion,
            String errorMessage
    ) {
        Analysis analysis = findAnalysisOrThrow(analysisId);

        analysis.fail(modelName, modelVersion, errorMessage);
    }

    private Patient findPatientOrThrow(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_MSG));
    }

    private void validatePatientCanUploadScans(Patient patient) {
        if (patient.isArchived()) {
            throw new InvalidPatientStateException(
                    "Cannot upload scans for archived patient."
            );
        }
    }

    private String createObjectKey(UUID analysisId, MultipartFile file) {
        return analysisObjectKeyFactory.create(
                analysisId,
                file.getOriginalFilename()
        );
    }

    private void uploadInputFile(String objectKey, MultipartFile file) {
        fileStorageService.upload(objectKey, file);
        uploadedFileCleanupService.deleteOnRollback(objectKey);
    }

    private Analysis createQueuedAnalysis(
            UUID analysisId,
            Patient patient,
            MultipartFile file,
            String objectKey,
            String modelName,
            String modelVersion
    ) {
        AnalysisInput analysisInput = createAnalysisInput(
                file,
                objectKey,
                modelName,
                modelVersion
        );

        return Analysis.queued(
                analysisId,
                patient,
                analysisInput
        );
    }

    private AnalysisInput createAnalysisInput(
            MultipartFile file,
            String objectKey,
            String modelName,
            String modelVersion
    ) {
        return new AnalysisInput(
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize(),
                modelName,
                modelVersion
        );
    }

    private Analysis saveAnalysisWithAuditAndOutbox(Analysis analysis) {
        Analysis savedAnalysis = analysisRepository.save(analysis);

        recordAnalysisUploadedAudit(savedAnalysis);
        outboxEventService.saveAnalysisRequestedEvent(savedAnalysis);

        return savedAnalysis;
    }

    private Analysis findAnalysisOrThrow(UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(ANALYSIS_NOT_FOUND_MSG));
    }

    private Analysis findAnalysisForRetryOrThrow(UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ANALYSIS_NOT_FOUND_WITH_ID_MSG + analysisId
                ));
    }

    private void validateAnalysisCanBeRetried(Analysis analysis) {
        if (analysis.getStatus() != AnalysisStatus.FAILED) {
            throw new InvalidAnalysisStateException(
                    "Only failed analyses can be retried."
            );
        }
    }

    private void recordAnalysisUploadedAudit(Analysis analysis) {
        auditEventService.recordEvent(
                AuditEventType.ANALYSIS_UPLOADED,
                analysis.getPatient().getId(),
                analysis.getId(),
                analysisUploadedMessage(analysis)
        );
    }

    private void recordAnalysisRetriedAudit(Analysis analysis) {
        auditEventService.recordEvent(
                AuditEventType.ANALYSIS_RETRIED,
                analysis.getPatient().getId(),
                analysis.getId(),
                analysisRetriedMessage(analysis)
        );
    }

    private String analysisUploadedMessage(Analysis analysis) {
        return "Scan %s was uploaded for analysis."
                .formatted(analysis.getOriginalFileName());
    }

    private String analysisRetriedMessage(Analysis analysis) {
        return "Analysis %s was retried."
                .formatted(analysis.getId());
    }
}
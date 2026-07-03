package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.InvalidPatientStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.validation.FileUploadValidator;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.AnalysisEventPublisher;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisDetectionPayload;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final String ANALYSIS_NOT_FOUND_MSG = "Analysis not found";
    private static final String PATIENT_NOT_FOUND_MSG = "Patient not found";
    private static final String DEFAULT_INPUT_FILENAME = "input";

    private final AnalysisRepository analysisRepository;
    private final PatientRepository patientRepository;
    private final FileStorageService fileStorageService;
    private final FileUploadValidator fileUploadValidator;
    private final AnalysisEventPublisher analysisEventPublisher;
    private final AuditEventService auditEventService;
    private final AnalysisMapper analysisMapper;

    @Transactional
    public AnalysisResponseDTO create(
            UUID patientId,
            MultipartFile file,
            String modelName,
            String modelVersion
    ) {
        fileUploadValidator.validateImageFile(file);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_MSG));

        if (patient.isArchived()) {
            throw new InvalidPatientStateException(
                    "Cannot upload scans for archived patient."
            );
        }

        UUID analysisId = UUID.randomUUID();
        String objectKey = buildObjectKey(analysisId, file.getOriginalFilename());

        fileStorageService.upload(objectKey, file);

        AnalysisInput analysisInput = new AnalysisInput(
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize(),
                modelName,
                modelVersion
        );

        Analysis analysis = Analysis.queued(
                analysisId,
                patient,
                analysisInput
        );

        Analysis savedAnalysis = analysisRepository.save(analysis);

        auditEventService.recordEvent(
                AuditEventType.ANALYSIS_UPLOADED,
                savedAnalysis.getPatient().getId(),
                savedAnalysis.getId(),
                "Scan " + savedAnalysis.getOriginalFileName() + " was uploaded for analysis."
        );

        publishAnalysisRequestedAfterCommit(savedAnalysis);

        return analysisMapper.toResponseDTO(savedAnalysis);
    }

    @Transactional(readOnly = true)
    public AnalysisResponseDTO findById(UUID id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ANALYSIS_NOT_FOUND_MSG));

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
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found with id: " + analysisId
                ));

        if (analysis.getStatus() != AnalysisStatus.FAILED) {
            throw new InvalidAnalysisStateException(
                    "Only failed analyses can be retried."
            );
        }

        analysis.retry();

        auditEventService.recordEvent(
                AuditEventType.ANALYSIS_RETRIED,
                analysis.getPatient().getId(),
                analysis.getId(),
                "Analysis " + analysis.getId() + " was retried."
        );

        publishAnalysisRequestedAfterCommit(analysis);

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
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(ANALYSIS_NOT_FOUND_MSG));

        analysis.complete(modelName, modelVersion, resultObjectKey, detections);
    }

    @Transactional
    public void fail(
            UUID analysisId,
            String modelName,
            String modelVersion,
            String errorMessage
    ) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(ANALYSIS_NOT_FOUND_MSG));

        analysis.fail(modelName, modelVersion, errorMessage);
    }

    private void publishAnalysisRequestedAfterCommit(Analysis analysis) {
        AnalysisRequestedEvent event = AnalysisRequestedEvent.create(
                analysis.getId(),
                analysis.getPatient().getId(),
                analysis.getObjectKey(),
                analysis.getModelName(),
                analysis.getModelVersion()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                analysisEventPublisher.publishAnalysisRequested(event);
            }
        });
    }

    private String buildObjectKey(UUID analysisId, String originalFilename) {
        String safeFilename = originalFilename == null
                ? DEFAULT_INPUT_FILENAME
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        return "analyses/%s/%s".formatted(analysisId, safeFilename);
    }
}
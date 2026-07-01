package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisDetectionDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
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

    private final AnalysisRepository analysisRepository;
    private final PatientRepository patientRepository;
    private final FileStorageService fileStorageService;
    private final FileUploadValidator fileUploadValidator;
    private final AnalysisEventPublisher analysisEventPublisher;

    @Transactional
    public AnalysisResponseDTO create(
            UUID patientId,
            MultipartFile file,
            String modelName,
            String modelVersion
    ) {
        fileUploadValidator.validateImageFile(file);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (patient.isArchived()) {
            throw new InvalidPatientStateException(
                    "Cannot upload scans for archived patient."
            );
        }

        UUID analysisId = UUID.randomUUID();
        String objectKey = buildObjectKey(analysisId, file.getOriginalFilename());

        fileStorageService.upload(objectKey, file);

        Analysis analysis = Analysis.queued(
                analysisId,
                patient,
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize(),
                modelName,
                modelVersion
        );

        Analysis savedAnalysis = analysisRepository.save(analysis);

        publishAnalysisRequestedAfterCommit(savedAnalysis);

        return toResponseDTO(savedAnalysis);
    }

    @Transactional(readOnly = true)
    public AnalysisResponseDTO findById(UUID id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));

        return toResponseDTO(analysis);
    }

    @Transactional(readOnly = true)
    public List<AnalysisListItemDTO> findAllAnalyses() {
        return analysisRepository.findAllAnalysisListItems();
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponseDTO> findByPatientId(UUID patientId) {
        return analysisRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentAnalysisDTO> findRecentAnalyses(int limit) {
        int safeLimit = Math.clamp(limit, 1, 20);
        Pageable pageable = PageRequest.of(0, safeLimit);

        return analysisRepository.findRecentAnalyses(pageable);
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

        publishAnalysisRequestedAfterCommit(analysis);

        return toResponseDTO(analysis);
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
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));

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
                ? "input"
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        return "analyses/%s/%s".formatted(analysisId, safeFilename);
    }

    private AnalysisResponseDTO toResponseDTO(Analysis analysis) {
        String originalImageUrl = fileStorageService.generatePresignedUrl(analysis.getObjectKey());

        String resultImageUrl = analysis.getResultObjectKey() == null
                ? null
                : fileStorageService.generatePresignedUrl(analysis.getResultObjectKey());

        return new AnalysisResponseDTO(
                analysis.getId(),
                analysis.getPatient().getId(),
                analysis.getStatus(),
                analysis.getOriginalFileName(),
                analysis.getObjectKey(),
                originalImageUrl,
                analysis.getResultObjectKey(),
                resultImageUrl,
                analysis.getContentType(),
                analysis.getFileSizeBytes(),
                analysis.getModelName(),
                analysis.getModelVersion(),
                analysis.getErrorMessage(),
                analysis.getCreatedAt(),
                analysis.getCompletedAt(),
                analysis.getDetections()
                        .stream()
                        .map(detection -> new AnalysisDetectionDTO(
                                detection.getId(),
                                detection.getLabel(),
                                detection.getConfidence(),
                                detection.getX(),
                                detection.getY(),
                                detection.getWidth(),
                                detection.getHeight()
                        ))
                        .toList()
        );
    }
}
package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper.AnalysisMapper;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisDetectionPayload;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Handles analysis lifecycle operations such as retrying,
 * completing, and failing analyses.
 */
@Service
@RequiredArgsConstructor
public class AnalysisLifecycleService {

    private static final String ANALYSIS_NOT_FOUND_MSG = "Analysis not found";
    private static final String ANALYSIS_NOT_FOUND_WITH_ID_MSG = "Analysis not found with id: ";

    private final AnalysisRepository analysisRepository;
    private final AuditEventService auditEventService;
    private final AnalysisMapper analysisMapper;
    private final OutboxEventService outboxEventService;

    @Transactional
    public AnalysisResponseDTO retryAnalysis(UUID analysisId) {
        Analysis analysis = findAnalysisForRetryOrThrow(analysisId);

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

        analysis.complete(
                modelName,
                modelVersion,
                resultObjectKey,
                detections);
    }

    @Transactional
    public void fail(
            UUID analysisId,
            String modelName,
            String modelVersion,
            String errorMessage) {
        Analysis analysis = findAnalysisOrThrow(analysisId);

        analysis.fail(
                modelName,
                modelVersion,
                errorMessage);
    }

    private Analysis findAnalysisOrThrow(UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ANALYSIS_NOT_FOUND_MSG));
    }

    private Analysis findAnalysisForRetryOrThrow(UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ANALYSIS_NOT_FOUND_WITH_ID_MSG + analysisId));
    }

    private void recordAnalysisRetriedAudit(Analysis analysis) {
        auditEventService.recordEvent(
                AuditEventType.ANALYSIS_RETRIED,
                analysis.getPatient().getId(),
                analysis.getId(),
                "Analysis %s was retried."
                        .formatted(analysis.getId()));
    }
}

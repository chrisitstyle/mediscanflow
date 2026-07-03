package com.chrisitstyle.mediscanflow.medicalplatform.analyses.mapper;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisDetection;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisDetectionDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection.AnalysisSummaryProjection;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisMapper {

    private final FileStorageService fileStorageService;

    public AnalysisResponseDTO toResponseDTO(Analysis analysis) {
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
                        .map(this::toDetectionDTO)
                        .toList()
        );
    }

    private AnalysisDetectionDTO toDetectionDTO(AnalysisDetection detection) {
        return new AnalysisDetectionDTO(
                detection.getId(),
                detection.getLabel(),
                detection.getConfidence(),
                detection.getX(),
                detection.getY(),
                detection.getWidth(),
                detection.getHeight()
        );
    }

    public AnalysisListItemDTO toListItemDTO(AnalysisSummaryProjection analysis) {
        return new AnalysisListItemDTO(
                analysis.getId(),
                analysis.getPatient().getId(),
                formatPatientName(analysis.getPatient()),
                analysis.getStatus(),
                analysis.getOriginalFileName(),
                analysis.getModelName(),
                analysis.getModelVersion(),
                analysis.getFileSizeBytes(),
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }

    public RecentAnalysisDTO toRecentAnalysisDTO(AnalysisSummaryProjection analysis) {
        return new RecentAnalysisDTO(
                analysis.getId(),
                analysis.getPatient().getId(),
                formatPatientName(analysis.getPatient()),
                analysis.getStatus(),
                analysis.getOriginalFileName(),
                analysis.getModelName(),
                analysis.getModelVersion(),
                analysis.getFileSizeBytes(),
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }

    private String formatPatientName(AnalysisSummaryProjection.PatientProjection patient) {
        return patient.getFirstName() + " " + patient.getLastName();
    }
}
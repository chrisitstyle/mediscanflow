package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.PatientRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final PatientRepository patientRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public AnalysisResponseDTO create(UUID patientId, MultipartFile file, String modelName, String modelVersion) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        UUID analysisId = UUID.randomUUID();
        String objectKey = buildObjectKey(analysisId, file.getOriginalFilename());

        fileStorageService.upload(objectKey, file);

        Analysis analysis = Analysis.uploaded(
                analysisId,
                patient,
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize(),
                modelName,
                modelVersion
        );

        Analysis saved = analysisRepository.save(analysis);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AnalysisResponseDTO findById(UUID id) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found"));

        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponseDTO> findByPatientId(UUID patientId) {
        return analysisRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String buildObjectKey(UUID analysisId, String originalFilename) {
        String safeFilename = originalFilename == null ? "input" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "analyses/%s/%s".formatted(analysisId, safeFilename);
    }

    private AnalysisResponseDTO toResponse(Analysis analysis) {
        return new AnalysisResponseDTO(
                analysis.getId(),
                analysis.getPatient().getId(),
                analysis.getStatus(),
                analysis.getOriginalFileName(),
                analysis.getObjectKey(),
                analysis.getContentType(),
                analysis.getFileSizeBytes(),
                analysis.getModelName(),
                analysis.getModelVersion(),
                analysis.getErrorMessage(),
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }
}

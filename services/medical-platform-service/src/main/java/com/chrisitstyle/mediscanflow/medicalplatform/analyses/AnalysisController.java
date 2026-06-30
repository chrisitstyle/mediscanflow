package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/patients/{patientId}/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    AnalysisResponseDTO create(
            @PathVariable UUID patientId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "brain-tumor-detector") String modelName,
            @RequestParam(defaultValue = "mock-v1") String modelVersion
    ) {
        return analysisService.create(patientId, file, modelName, modelVersion);
    }

    @GetMapping("/analyses/{id}")
    AnalysisResponseDTO findById(@PathVariable UUID id) {
        return analysisService.findById(id);
    }

    @GetMapping("/patients/{patientId}/analyses")
    List<AnalysisResponseDTO> findByPatientId(@PathVariable UUID patientId) {
        return analysisService.findByPatientId(patientId);
    }
}

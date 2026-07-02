package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisReportService analysisReportService;

    @PostMapping("/patients/{patientId}/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    AnalysisResponseDTO create(
            @PathVariable UUID patientId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "yolo-brain-tumor-detector") String modelName,
            @RequestParam(defaultValue = "yolov8n") String modelVersion
    ) {
        return analysisService.create(patientId, file, modelName, modelVersion);
    }

    @GetMapping("/analyses/recent")
    List<RecentAnalysisDTO> getRecentAnalyses(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return analysisService.findRecentAnalyses(limit);
    }

    @GetMapping("/analyses")
    List<AnalysisListItemDTO> findAllAnalyses() {
        return analysisService.findAllAnalyses();
    }

    @GetMapping("/analyses/{id}")
    AnalysisResponseDTO findById(@PathVariable UUID id) {
        return analysisService.findById(id);
    }

    @PostMapping("analyses/{id}/retry")
    AnalysisResponseDTO retryAnalysis(@PathVariable UUID id) {
        return analysisService.retryAnalysis(id);
    }

    @GetMapping("/patients/{patientId}/analyses")
    List<AnalysisResponseDTO> findByPatientId(@PathVariable UUID patientId) {
        return analysisService.findByPatientId(patientId);
    }

    @GetMapping("/analyses/{analysisId}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID analysisId) {
        byte[] report = analysisReportService.generateReport(analysisId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("analysis-report-" + analysisId + ".pdf")
                        .build()
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(report);
    }

}

package com.chrisitstyle.mediscanflow.medicalplatform.analyses.report;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnalysisReportService {

    private final AnalysisRepository analysisRepository;
    private final AuditEventService auditEventService;
    private final AnalysisReportPdfRenderer pdfRenderer;

    AnalysisReportService(
            AnalysisRepository analysisRepository,
            AuditEventService auditEventService,
            AnalysisReportPdfRenderer pdfRenderer
    ) {
        this.analysisRepository = analysisRepository;
        this.auditEventService = auditEventService;
        this.pdfRenderer = pdfRenderer;
    }

    @Transactional
    public byte[] generateReport(UUID analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + analysisId));

        byte[] report = pdfRenderer.render(analysis);

        auditEventService.recordEvent(
                AuditEventType.REPORT_DOWNLOADED,
                analysis.getPatient().getId(),
                analysis.getId(),
                "PDF report was downloaded for analysis " + analysis.getId() + "."
        );

        return report;
    }
}
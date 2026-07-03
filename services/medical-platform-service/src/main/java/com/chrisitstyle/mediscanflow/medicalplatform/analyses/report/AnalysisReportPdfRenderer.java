package com.chrisitstyle.mediscanflow.medicalplatform.analyses.report;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
class AnalysisReportPdfRenderer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisReportPdfRenderer.class);

    private final AnalysisReportSectionRenderer sectionRenderer;

    AnalysisReportPdfRenderer(AnalysisReportSectionRenderer sectionRenderer) {
        this.sectionRenderer = sectionRenderer;
    }

    byte[] render(Analysis analysis) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            sectionRenderer.addHeader(document, analysis);
            sectionRenderer.addOverviewSection(document, analysis);
            sectionRenderer.addImagesSection(document, analysis);
            sectionRenderer.addDetectionsSection(document, analysis.getDetections());
            sectionRenderer.addFooter(document);

            document.close();

            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            log.error("Could not generate analysis report for analysisId={}", analysis.getId(), exception);
            throw new IllegalStateException("Could not generate analysis report.", exception);
        }
    }
}
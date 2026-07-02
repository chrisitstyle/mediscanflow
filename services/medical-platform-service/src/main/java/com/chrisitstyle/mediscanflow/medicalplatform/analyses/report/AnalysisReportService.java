package com.chrisitstyle.mediscanflow.medicalplatform.analyses.report;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisDetection;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisRepository;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AnalysisReportService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisReportService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                    .withZone(ZoneOffset.UTC);

    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color CARD_BACKGROUND = new Color(248, 250, 252);
    private static final Color TABLE_HEADER_BACKGROUND = new Color(241, 245, 249);

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color WARNING = new Color(202, 138, 4);
    private static final Color DANGER = new Color(220, 38, 38);
    private static final Color NEUTRAL = new Color(71, 85, 105);
    private static final Color WHITE = Color.WHITE;

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, TEXT_DARK);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_MUTED);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, TEXT_DARK);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
    private static final Font VALUE_BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_DARK);
    private static final Font TABLE_BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
    private static final Font STATUS_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, WHITE);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);

    private final AnalysisRepository analysisRepository;
    private final FileStorageService storageService;

    public AnalysisReportService(
            AnalysisRepository analysisRepository,
            FileStorageService storageService
    ) {
        this.analysisRepository = analysisRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public byte[] generateReport(UUID analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with id: " + analysisId));

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addHeader(document, analysis);
            addOverviewSection(document, analysis);
            addImagesSection(document, analysis);
            addDetectionsSection(document, analysis.getDetections());
            addFooter(document);

            document.close();

            return outputStream.toByteArray();
        } catch (Exception exception) {
            log.error("Could not generate analysis report for analysisId={}", analysisId, exception);
            throw new IllegalStateException("Could not generate analysis report.", exception);
        }
    }

    private void addHeader(Document document, Analysis analysis) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{4.2F, 1.2F});
        headerTable.setSpacingAfter(18);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Paragraph title = new Paragraph("MediScanFlow Analysis Report", TITLE_FONT);
        title.setSpacingAfter(6);
        leftCell.addElement(title);

        Paragraph subtitle = new Paragraph(
                "Generated automatically from the AI medical scan workflow.",
                SUBTITLE_FONT
        );
        subtitle.setSpacingAfter(8);
        leftCell.addElement(subtitle);

        Paragraph analysisId = new Paragraph("Analysis ID: " + analysis.getId(), SUBTITLE_FONT);
        leftCell.addElement(analysisId);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingTop(4);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(100);
        badgeTable.addCell(createStatusBadgeCell(analysis.getStatus()));
        rightCell.addElement(badgeTable);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    private void addOverviewSection(Document document, Analysis analysis) throws Exception {
        PdfPTable overviewTable = new PdfPTable(2);
        overviewTable.setWidthPercentage(100);
        overviewTable.setWidths(new float[]{1, 1});
        overviewTable.setSpacingAfter(16);

        overviewTable.addCell(createInfoCard("Patient", createPatientTable(analysis.getPatient())));
        overviewTable.addCell(createInfoCard("Analysis", createAnalysisTable(analysis)));

        document.add(overviewTable);
    }

    private PdfPTable createPatientTable(Patient patient) throws Exception {
        PdfPTable table = createKeyValueTable();

        addKeyValueRow(table, "First name", patient.getFirstName());
        addKeyValueRow(table, "Last name", patient.getLastName());
        addKeyValueRow(table, "Date of birth", patient.getDateOfBirth().toString());
        addKeyValueRow(table, "Medical record number", patient.getMedicalRecordNumber());

        return table;
    }

    private PdfPTable createAnalysisTable(Analysis analysis) throws Exception {
        PdfPTable table = createKeyValueTable();

        addKeyValueRow(table, "Status", analysis.getStatus().name());
        addKeyValueRow(table, "Model", analysis.getModelName());
        addKeyValueRow(table, "Model version", analysis.getModelVersion());
        addKeyValueRow(table, "Original filename", analysis.getOriginalFileName());
        addKeyValueRow(table, "Content type", analysis.getContentType());
        addKeyValueRow(table, "File size", formatFileSize(analysis.getFileSizeBytes()));
        addKeyValueRow(table, "Created at", DATE_TIME_FORMATTER.format(analysis.getCreatedAt()));

        if (analysis.getCompletedAt() != null) {
            addKeyValueRow(table, "Completed at", DATE_TIME_FORMATTER.format(analysis.getCompletedAt()));
        }

        if (analysis.getErrorMessage() != null && !analysis.getErrorMessage().isBlank()) {
            addKeyValueRow(table, "Error message", analysis.getErrorMessage());
        }

        return table;
    }

    private void addImagesSection(Document document, Analysis analysis) throws Exception {
        Paragraph sectionTitle = new Paragraph("Images", SECTION_FONT);
        sectionTitle.setSpacingBefore(4);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        PdfPTable imageTable = new PdfPTable(2);
        imageTable.setWidthPercentage(100);
        imageTable.setWidths(new float[]{1, 1});
        imageTable.setSpacingAfter(16);

        imageTable.addCell(createImageCard("Original scan", analysis.getObjectKey()));

        if (analysis.getResultObjectKey() == null || analysis.getResultObjectKey().isBlank()) {
            imageTable.addCell(createPlaceholderImageCard("AI result", "AI result image is not available yet."));
        } else {
            imageTable.addCell(createImageCard("AI result", analysis.getResultObjectKey()));
        }

        document.add(imageTable);
    }

    private void addDetectionsSection(
            Document document,
            List<AnalysisDetection> detections
    ) throws Exception {
        Paragraph sectionTitle = new Paragraph("Detections", SECTION_FONT);
        sectionTitle.setSpacingBefore(4);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        if (detections == null || detections.isEmpty()) {
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(100);
            table.setSpacingAfter(16);

            PdfPCell cell = new PdfPCell(new Phrase("No detections available for this analysis.", VALUE_FONT));
            cell.setPadding(12);
            cell.setBorder(Rectangle.BOX);
            cell.setBorderColor(BORDER);
            cell.setBackgroundColor(CARD_BACKGROUND);

            table.addCell(cell);
            document.add(table);
            return;
        }

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingAfter(16);
        table.setWidths(new float[]{2.2F, 1.4F, 1.1F, 1.1F, 1.1F, 1.1F});

        addDetectionHeaderCell(table, "Label");
        addDetectionHeaderCell(table, "Confidence");
        addDetectionHeaderCell(table, "X");
        addDetectionHeaderCell(table, "Y");
        addDetectionHeaderCell(table, "Width");
        addDetectionHeaderCell(table, "Height");

        for (AnalysisDetection detection : detections) {
            addDetectionBodyCell(table, detection.getLabel(), false);
            addDetectionBodyCell(table, String.format(Locale.US, "%.2f%%", detection.getConfidence() * 100), true);
            addDetectionBodyCell(table, String.format(Locale.US, "%.2f", detection.getX()), true);
            addDetectionBodyCell(table, String.format(Locale.US, "%.2f", detection.getY()), true);
            addDetectionBodyCell(table, String.format(Locale.US, "%.2f", detection.getWidth()), true);
            addDetectionBodyCell(table, String.format(Locale.US, "%.2f", detection.getHeight()), true);
        }

        document.add(table);
    }

    private void addFooter(Document document) throws Exception {
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);
        footerTable.setSpacingBefore(8);

        String generatedAt = DATE_TIME_FORMATTER.format(Instant.now());

        PdfPCell footerCell = new PdfPCell(new Phrase(
                "Generated by MediScanFlow on " + generatedAt + ". This report was created automatically from stored analysis data.",
                FOOTER_FONT
        ));
        footerCell.setBorder(Rectangle.TOP);
        footerCell.setBorderColor(BORDER);
        footerCell.setPaddingTop(10);

        footerTable.addCell(footerCell);
        document.add(footerTable);
    }

    private PdfPCell createInfoCard(String title, PdfPTable contentTable) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(CARD_BACKGROUND);

        Paragraph titleParagraph = new Paragraph(title, SECTION_FONT);
        titleParagraph.setSpacingAfter(8);
        cell.addElement(titleParagraph);

        cell.addElement(contentTable);

        return cell;
    }

    private PdfPTable createKeyValueTable() throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.45F, 2.4F});

        return table;
    }

    private void addKeyValueRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, LABEL_FONT));
        keyCell.setBorder(Rectangle.NO_BORDER);
        keyCell.setPaddingBottom(6);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "N/A" : value, VALUE_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6);

        table.addCell(keyCell);
        table.addCell(valueCell);
    }

    private PdfPCell createImageCard(String title, String objectKey) {
        PdfPCell cell = createBaseImageCardCell(title);

        try {
            byte[] imageBytes = storageService.download(objectKey);
            Image image = Image.getInstance(imageBytes);
            image.setAlignment(Element.ALIGN_CENTER);
            image.scaleToFit(220, 220);

            cell.addElement(image);
        } catch (Exception exception) {
            log.warn("Could not add image to analysis report. objectKey={}", objectKey, exception);
            cell.addElement(new Paragraph("Could not load image.", VALUE_FONT));
        }

        return cell;
    }

    private PdfPCell createPlaceholderImageCard(String title, String text) {
        PdfPCell cell = createBaseImageCardCell(title);

        Paragraph paragraph = new Paragraph(text, VALUE_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(42);

        cell.addElement(paragraph);

        return cell;
    }

    private PdfPCell createBaseImageCardCell(String title) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(WHITE);
        cell.setMinimumHeight(250);

        Paragraph titleParagraph = new Paragraph(title, VALUE_BOLD_FONT);
        titleParagraph.setSpacingAfter(8);
        cell.addElement(titleParagraph);

        return cell;
    }

    private PdfPCell createStatusBadgeCell(AnalysisStatus status) {
        PdfPCell cell = new PdfPCell(new Phrase(status.name(), STATUS_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(getStatusColor(status));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);

        return cell;
    }

    private Color getStatusColor(AnalysisStatus status) {
        return switch (status) {
            case COMPLETED -> SUCCESS;
            case FAILED -> DANGER;
            case QUEUED -> WARNING;
            case PROCESSING -> PRIMARY;
            case UPLOADED -> NEUTRAL;
        };
    }

    private void addDetectionHeaderCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, TABLE_HEADER_FONT));
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(TABLE_HEADER_BACKGROUND);
        table.addCell(cell);
    }

    private void addDetectionBodyCell(PdfPTable table, String value, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "N/A" : value, TABLE_BODY_FONT));
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);

        if (alignRight) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }

        table.addCell(cell);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        }

        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB (%d bytes)", bytes / 1024.0, bytes);
        }

        return String.format(Locale.US, "%.1f MB (%d bytes)", bytes / (1024.0 * 1024.0), bytes);
    }
}
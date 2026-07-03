package com.chrisitstyle.mediscanflow.medicalplatform.analyses.report;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisDetection;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.BORDER;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.CARD_BACKGROUND;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.DATE_TIME_FORMATTER;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.LABEL_FONT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.SECTION_FONT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.TABLE_BODY_FONT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.TABLE_HEADER_BACKGROUND;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.TABLE_HEADER_FONT;
import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.VALUE_FONT;

@Component
class AnalysisReportTableFactory {

    private static final String NOT_AVAILABLE = "N/A";

    private static final long BYTES_IN_KILOBYTE = 1024L;
    private static final long BYTES_IN_MEGABYTE = BYTES_IN_KILOBYTE * BYTES_IN_KILOBYTE;

    PdfPCell createInfoCard(String title, PdfPTable contentTable) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(CARD_BACKGROUND);

        Phrase titlePhrase = new Phrase(title, SECTION_FONT);
        cell.addElement(titlePhrase);
        cell.addElement(contentTable);

        return cell;
    }

    PdfPTable createPatientTable(Patient patient) throws DocumentException {
        PdfPTable table = createKeyValueTable();

        addKeyValueRow(table, "First name", patient.getFirstName());
        addKeyValueRow(table, "Last name", patient.getLastName());
        addKeyValueRow(table, "Date of birth", patient.getDateOfBirth().toString());
        addKeyValueRow(table, "Medical record number", patient.getMedicalRecordNumber());

        return table;
    }

    PdfPTable createAnalysisTable(Analysis analysis) throws DocumentException {
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

    PdfPTable createNoDetectionsTable() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(16);

        PdfPCell cell = new PdfPCell(new Phrase("No detections available for this analysis.", VALUE_FONT));
        cell.setPadding(12);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(CARD_BACKGROUND);

        table.addCell(cell);

        return table;
    }

    PdfPTable createDetectionsTable(List<AnalysisDetection> detections) throws DocumentException {
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

        return table;
    }

    private PdfPTable createKeyValueTable() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.45F, 2.4F});

        return table;
    }

    private void addKeyValueRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, LABEL_FONT));
        keyCell.setBorder(Rectangle.NO_BORDER);
        keyCell.setPaddingBottom(6);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null || value.isBlank() ? NOT_AVAILABLE : value, VALUE_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6);

        table.addCell(keyCell);
        table.addCell(valueCell);
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
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? NOT_AVAILABLE : value, TABLE_BODY_FONT));
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);

        if (alignRight) {
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        }

        table.addCell(cell);
    }

    private String formatFileSize(long bytes) {
        if (bytes < BYTES_IN_KILOBYTE) {
            return bytes + " bytes";
        }

        if (bytes < BYTES_IN_MEGABYTE) {
            return String.format(Locale.US, "%.1f KB (%d bytes)", bytes / (double) BYTES_IN_KILOBYTE, bytes);
        }

        return String.format(Locale.US, "%.1f MB (%d bytes)", bytes / (double) BYTES_IN_MEGABYTE, bytes);
    }
}
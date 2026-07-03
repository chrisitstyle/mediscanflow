package com.chrisitstyle.mediscanflow.medicalplatform.analyses.report;

import com.chrisitstyle.mediscanflow.medicalplatform.storage.FileStorageService;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.report.AnalysisReportStyles.*;

@Component
class AnalysisReportImageFactory {

    private static final Logger log = LoggerFactory.getLogger(AnalysisReportImageFactory.class);

    private static final float IMAGE_MAX_WIDTH = 220F;
    private static final float IMAGE_MAX_HEIGHT = 220F;
    private static final float PLACEHOLDER_SPACING_BEFORE = 42F;
    private static final float CARD_MIN_HEIGHT = 250F;

    private final FileStorageService storageService;

    AnalysisReportImageFactory(FileStorageService storageService) {
        this.storageService = storageService;
    }

    PdfPCell createImageCard(String title, String objectKey) {
        PdfPCell cell = createBaseImageCardCell(title);

        try {
            byte[] imageBytes = storageService.download(objectKey);
            Image image = Image.getInstance(imageBytes);
            image.setAlignment(Element.ALIGN_CENTER);
            image.scaleToFit(IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);

            cell.addElement(image);
        } catch (BadElementException | IOException | IllegalStateException exception) {
            log.warn("Could not add image to analysis report. objectKey={}", objectKey, exception);
            cell.addElement(new Paragraph("Could not load image.", VALUE_FONT));
        }

        return cell;
    }

    PdfPCell createPlaceholderImageCard(String title, String text) {
        PdfPCell cell = createBaseImageCardCell(title);

        Paragraph paragraph = new Paragraph(text, VALUE_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(PLACEHOLDER_SPACING_BEFORE);

        cell.addElement(paragraph);

        return cell;
    }

    private PdfPCell createBaseImageCardCell(String title) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(WHITE);
        cell.setMinimumHeight(CARD_MIN_HEIGHT);

        Paragraph titleParagraph = new Paragraph(title, VALUE_BOLD_FONT);
        titleParagraph.setSpacingAfter(8);
        cell.addElement(titleParagraph);

        return cell;
    }
}
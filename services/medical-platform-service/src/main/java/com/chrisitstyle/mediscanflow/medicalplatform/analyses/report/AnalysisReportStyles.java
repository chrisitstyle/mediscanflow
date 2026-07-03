package com.chrisitstyle.mediscanflow.medicalplatform.analyses.report;

import org.openpdf.text.Font;

import java.awt.Color;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class AnalysisReportStyles {

    static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                    .withZone(ZoneOffset.UTC);

    static final Color TEXT_DARK = new Color(15, 23, 42);
    static final Color TEXT_MUTED = new Color(100, 116, 139);
    static final Color BORDER = new Color(226, 232, 240);
    static final Color CARD_BACKGROUND = new Color(248, 250, 252);
    static final Color TABLE_HEADER_BACKGROUND = new Color(241, 245, 249);

    static final Color PRIMARY = new Color(37, 99, 235);
    static final Color SUCCESS = new Color(22, 163, 74);
    static final Color WARNING = new Color(202, 138, 4);
    static final Color DANGER = new Color(220, 38, 38);
    static final Color NEUTRAL = new Color(71, 85, 105);
    static final Color WHITE = Color.WHITE;

    static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, TEXT_DARK);
    static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_MUTED);
    static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, TEXT_DARK);
    static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED);
    static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
    static final Font VALUE_BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
    static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_DARK);
    static final Font TABLE_BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
    static final Font STATUS_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, WHITE);
    static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);

    private AnalysisReportStyles() {
    }
}
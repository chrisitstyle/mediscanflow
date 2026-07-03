package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates storage object keys for analysis input files.
 */
@Component
public class AnalysisObjectKeyFactory {

    private static final String DEFAULT_INPUT_FILENAME = "input";

    public String create(UUID analysisId, String originalFilename) {
        String safeFilename = originalFilename == null
                ? DEFAULT_INPUT_FILENAME
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        return "analyses/%s/%s".formatted(analysisId, safeFilename);
    }
}

package com.chrisitstyle.mediscanflow.medicalplatform.common.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "app.validation.file-upload")
public record FileUploadValidationProperties(
        long maxSizeBytes,
        Set<String> allowedContentTypes
) {

    public FileUploadValidationProperties {
        if (maxSizeBytes <= 0) {
            maxSizeBytes = 10 * 1024 * 1024;
        }

        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = Set.of(
                    "image/jpeg",
                    "image/png"
            );
        } else {
            allowedContentTypes = Set.copyOf(allowedContentTypes);
        }
    }
}

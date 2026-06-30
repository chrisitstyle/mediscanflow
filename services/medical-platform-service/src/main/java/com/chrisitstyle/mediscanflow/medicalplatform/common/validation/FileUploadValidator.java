package com.chrisitstyle.mediscanflow.medicalplatform.common.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileUploadValidator {

    private final FileUploadValidationProperties properties;

    public void validateImageFile(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileUploadException("File is required");
        }

        if (file.isEmpty()) {
            throw new InvalidFileUploadException("File must not be empty");
        }

        if (file.getSize() > properties.maxSizeBytes()) {
            throw new InvalidFileUploadException(
                    "File size exceeds the maximum allowed size of %d bytes"
                            .formatted(properties.maxSizeBytes())
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileUploadException("Original file name is required");
        }

        if (originalFilename.contains("..")) {
            throw new InvalidFileUploadException("Invalid file name");
        }

        String contentType = file.getContentType();

        if (contentType == null || !properties.allowedContentTypes().contains(contentType)) {
            throw new InvalidFileUploadException(
                    "Unsupported file type: %s. Allowed types: %s"
                            .formatted(contentType, properties.allowedContentTypes())
            );
        }
    }
}
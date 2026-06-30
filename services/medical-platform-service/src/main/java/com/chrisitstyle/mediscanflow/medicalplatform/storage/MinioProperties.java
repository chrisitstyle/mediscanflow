package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.minio")
public record MinioProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region
) {
}

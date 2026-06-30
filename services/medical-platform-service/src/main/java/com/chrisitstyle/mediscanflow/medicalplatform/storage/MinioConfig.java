package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
class MinioConfig {

    private final MinioProperties properties;

    @Bean
    @Qualifier("internalMinioClient")
    MinioClient internalMinioClient() {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    @Qualifier("publicMinioClient")
    MinioClient publicMinioClient() {
        return MinioClient.builder()
                .endpoint(properties.publicEndpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }
}

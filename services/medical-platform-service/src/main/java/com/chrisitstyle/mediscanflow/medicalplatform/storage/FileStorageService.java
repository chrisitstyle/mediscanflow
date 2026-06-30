package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    void upload(String objectKey, MultipartFile file);
    String generatePresignedUrl(String objectKey);
}

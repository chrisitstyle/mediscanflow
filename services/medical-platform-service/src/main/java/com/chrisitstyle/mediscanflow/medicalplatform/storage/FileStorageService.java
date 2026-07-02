package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    byte[] download(String objectKey);
    void upload(String objectKey, MultipartFile file);
    String generatePresignedUrl(String objectKey);
}

package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public void upload(String objectKey, MultipartFile file) {
        try {
            ensureBucketExists();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(file.getInputStream(),
                            file.getSize(), (long) -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not upload file to MinIO", exception);
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate presigned URL for object: " + objectKey, exception);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }
}

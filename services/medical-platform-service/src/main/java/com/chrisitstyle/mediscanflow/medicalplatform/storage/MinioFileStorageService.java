package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import io.minio.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Service
class MinioFileStorageService implements FileStorageService {

    private final MinioClient internalMinioClient;
    private final MinioClient publicMinioClient;
    private final MinioProperties properties;

    MinioFileStorageService(
            @Qualifier("internalMinioClient") MinioClient internalMinioClient,
            @Qualifier("publicMinioClient") MinioClient publicMinioClient,
            MinioProperties properties
    ) {
        this.internalMinioClient = internalMinioClient;
        this.publicMinioClient = publicMinioClient;
        this.properties = properties;
    }

    @Override
    public void upload(String objectKey, MultipartFile file) {
        try {
            ensureBucketExists();

            internalMinioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), (long) -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not upload file to MinIO", exception);
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey) {
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not generate presigned URL for object: " + objectKey,
                    exception
            );
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = internalMinioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.bucket())
                        .build()
        );

        if (!exists) {
            internalMinioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(properties.bucket())
                            .build()
            );
        }
    }
}
package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class UploadedFileCleanupService {

    private final FileStorageService fileStorageService;

    public void deleteOnRollback(String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    fileStorageService.deleteIfExists(objectKey);
                }
            }
        });
    }

    public void deleteIfNoActiveTransaction(String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.deleteIfExists(objectKey);
        }
    }
}
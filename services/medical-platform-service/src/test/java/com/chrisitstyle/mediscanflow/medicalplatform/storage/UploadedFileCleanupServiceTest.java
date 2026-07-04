package com.chrisitstyle.mediscanflow.medicalplatform.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UploadedFileCleanupServiceTest {

    private static final String OBJECT_KEY =
            "analyses/4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24/brain-scan.jpg";

    private FileStorageService fileStorageService;
    private UploadedFileCleanupService uploadedFileCleanupService;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        uploadedFileCleanupService = new UploadedFileCleanupService(fileStorageService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteOnRollbackDeletesFileWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        uploadedFileCleanupService.deleteOnRollback(OBJECT_KEY);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK
                ));

        verify(fileStorageService).deleteIfExists(OBJECT_KEY);
    }

    @Test
    void deleteOnRollbackDoesNotDeleteFileWhenTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        uploadedFileCleanupService.deleteOnRollback(OBJECT_KEY);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_COMMITTED
                ));

        verify(fileStorageService, never()).deleteIfExists(OBJECT_KEY);
    }

    @Test
    void deleteOnRollbackDoesNothingWhenTransactionSynchronizationIsNotActive() {
        uploadedFileCleanupService.deleteOnRollback(OBJECT_KEY);

        verify(fileStorageService, never()).deleteIfExists(OBJECT_KEY);
    }

    @Test
    void deleteIfNoActiveTransactionDeletesFileWhenSynchronizationIsNotActive() {
        uploadedFileCleanupService.deleteIfNoActiveTransaction(OBJECT_KEY);

        verify(fileStorageService).deleteIfExists(OBJECT_KEY);
    }

    @Test
    void deleteIfNoActiveTransactionDoesNotDeleteFileWhenSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        uploadedFileCleanupService.deleteIfNoActiveTransaction(OBJECT_KEY);

        verify(fileStorageService, never()).deleteIfExists(OBJECT_KEY);
    }
}

package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.InvalidAnalysisStateException;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisDetectionPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.*;
import static com.chrisitstyle.mediscanflow.medicalplatform.testentities.PatientTestEntity.patient;
import static org.junit.jupiter.api.Assertions.*;

class AnalysisLifecycleTest {

    private static final String RESULT_OBJECT_KEY = "analyses/%s/result.jpg".formatted(ANALYSIS_ID);
    private static final String FAILURE_MESSAGE = "Simulated inference failure";

    @Test
    void startProcessingMovesQueuedAnalysisToProcessing() {
        Analysis analysis = queuedAnalysis();

        UUID attemptId = analysis.getProcessingAttemptId();

        analysis.startProcessing();

        assertEquals(AnalysisStatus.PROCESSING, analysis.getStatus());
        assertEquals(attemptId, analysis.getProcessingAttemptId());
    }

    @Test
    void startProcessingIsIdempotentWhenAlreadyProcessing() {
        Analysis analysis = queuedAnalysis();

        UUID attemptId = analysis.getProcessingAttemptId();

        analysis.startProcessing();
        analysis.startProcessing();

        assertEquals(AnalysisStatus.PROCESSING, analysis.getStatus());
        assertEquals(attemptId, analysis.getProcessingAttemptId());
    }

    @Test
    void startProcessingDoesNotOverwriteCompletedAnalysis() {
        Analysis analysis = completedAnalysis();

        UUID attemptId = analysis.getProcessingAttemptId();

        analysis.startProcessing();

        assertEquals(AnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(attemptId, analysis.getProcessingAttemptId());
    }

    @Test
    void startProcessingDoesNotOverwriteFailedAnalysis() {
        Analysis analysis = failedAnalysis();

        UUID attemptId = analysis.getProcessingAttemptId();

        analysis.startProcessing();

        assertEquals(AnalysisStatus.FAILED, analysis.getStatus());
        assertEquals(attemptId, analysis.getProcessingAttemptId());
    }

    @Test
    void queuedAnalysisHasProcessingAttemptId() {
        Analysis analysis = queuedAnalysis();

        assertNotNull(analysis.getProcessingAttemptId());
    }

    @Test
    void uploadedAnalysisHasNoProcessingAttemptId() {
        Analysis analysis = uploadedAnalysis();

        assertNull(analysis.getProcessingAttemptId());
    }

    @Test
    void retryMovesFailedAnalysisBackToQueuedAndClearsFailureState() {
        Analysis analysis = failedAnalysis();

        analysis.retry();

        assertEquals(AnalysisStatus.QUEUED, analysis.getStatus());
        assertNotNull(analysis.getProcessingAttemptId());
        assertNull(analysis.getModelName());
        assertNull(analysis.getModelVersion());
        assertNull(analysis.getErrorMessage());
        assertNull(analysis.getCompletedAt());
        assertNull(analysis.getResultObjectKey());
        assertEquals(0, analysis.getDetections().size());
    }

    @Test
    void retryCreatesNewProcessingAttempt() {
        Analysis analysis = failedAnalysis();

        UUID previousAttemptId = analysis.getProcessingAttemptId();

        analysis.retry();

        assertNotNull(analysis.getProcessingAttemptId());
        assertNotEquals(previousAttemptId, analysis.getProcessingAttemptId());
    }

    @Test
    void recognizesCurrentProcessingAttempt() {
        Analysis analysis = queuedAnalysis();

        UUID currentAttemptId = analysis.getProcessingAttemptId();

        assertTrue(analysis.isCurrentProcessingAttempt(currentAttemptId));

        assertFalse(analysis.isCurrentProcessingAttempt(UUID.randomUUID()));
    }

    @Test
    void uploadedAnalysisDoesNotRecognizeProcessingAttempt() {
        Analysis analysis = uploadedAnalysis();

        assertFalse(analysis.isCurrentProcessingAttempt(UUID.randomUUID()));
    }

    @Test
    void retryThrowsWhenAnalysisIsQueued() {
        Analysis analysis = queuedAnalysis();

        assertThrows(
                InvalidAnalysisStateException.class,
                analysis::retry);
    }

    @Test
    void retryThrowsWhenAnalysisIsCompleted() {
        Analysis analysis = completedAnalysis();

        assertThrows(
                InvalidAnalysisStateException.class,
                analysis::retry);
    }

    @Test
    void completeMovesQueuedAnalysisToCompleted() {
        Analysis analysis = queuedAnalysis();

        UUID processingAttemptId = analysis.getProcessingAttemptId();

        analysis.complete(
                MODEL_NAME,
                MODEL_VERSION,
                RESULT_OBJECT_KEY,
                detections());

        assertEquals(AnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(
                processingAttemptId,
                analysis.getProcessingAttemptId());
        assertEquals(MODEL_NAME, analysis.getModelName());
        assertEquals(MODEL_VERSION, analysis.getModelVersion());
        assertEquals(RESULT_OBJECT_KEY, analysis.getResultObjectKey());
        assertNotNull(analysis.getCompletedAt());
        assertNull(analysis.getErrorMessage());
        assertEquals(1, analysis.getDetections().size());
    }

    @Test
    void completeIsIdempotentWhenAnalysisIsAlreadyCompleted() {
        Analysis analysis = completedAnalysis();

        UUID originalAttemptId = analysis.getProcessingAttemptId();
        String originalResultObjectKey = analysis.getResultObjectKey();
        int originalDetectionCount = analysis.getDetections().size();

        analysis.complete(
                "different-model",
                "different-version",
                "different-result.jpg",
                List.of());

        assertEquals(AnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(
                originalAttemptId,
                analysis.getProcessingAttemptId());
        assertEquals(MODEL_NAME, analysis.getModelName());
        assertEquals(MODEL_VERSION, analysis.getModelVersion());
        assertEquals(
                originalResultObjectKey,
                analysis.getResultObjectKey());
        assertEquals(
                originalDetectionCount,
                analysis.getDetections().size());
    }

    @Test
    void completeThrowsWhenAnalysisIsFailed() {
        Analysis analysis = failedAnalysis();
        List<AnalysisDetectionPayload> detections = detections();

        assertThrows(InvalidAnalysisStateException.class,
                () -> analysis.complete(
                        MODEL_NAME,
                        MODEL_VERSION,
                        RESULT_OBJECT_KEY,
                        detections));
    }

    @Test
    void completeThrowsWhenAnalysisIsUploaded() {
        Analysis analysis = uploadedAnalysis();
        List<AnalysisDetectionPayload> detections = detections();

        assertThrows(InvalidAnalysisStateException.class,
                () -> analysis.complete(
                        MODEL_NAME,
                        MODEL_VERSION,
                        RESULT_OBJECT_KEY,
                        detections));
    }

    @Test
    void failMovesQueuedAnalysisToFailed() {
        Analysis analysis = queuedAnalysis();

        UUID processingAttemptId = analysis.getProcessingAttemptId();

        analysis.fail(
                MODEL_NAME,
                MODEL_VERSION,
                FAILURE_MESSAGE);

        assertEquals(AnalysisStatus.FAILED, analysis.getStatus());
        assertEquals(processingAttemptId, analysis.getProcessingAttemptId());
        assertEquals(MODEL_NAME, analysis.getModelName());
        assertEquals(MODEL_VERSION, analysis.getModelVersion());
        assertEquals(FAILURE_MESSAGE, analysis.getErrorMessage());
        assertNotNull(analysis.getCompletedAt());
        assertNull(analysis.getResultObjectKey());
        assertEquals(0, analysis.getDetections().size());
    }

    @Test
    void failIsIdempotentWhenAnalysisIsAlreadyFailed() {
        Analysis analysis = failedAnalysis();

        UUID originalAttemptId = analysis.getProcessingAttemptId();
        String originalModelName = analysis.getModelName();
        String originalModelVersion = analysis.getModelVersion();
        String originalErrorMessage = analysis.getErrorMessage();
        var originalCompletedAt = analysis.getCompletedAt();

        analysis.fail(
                "different-model",
                "different-version",
                "Different failure");

        assertEquals(AnalysisStatus.FAILED, analysis.getStatus());
        assertEquals(
                originalAttemptId,
                analysis.getProcessingAttemptId());
        assertEquals(originalModelName, analysis.getModelName());
        assertEquals(
                originalModelVersion,
                analysis.getModelVersion());
        assertEquals(
                originalErrorMessage,
                analysis.getErrorMessage());
        assertEquals(
                originalCompletedAt,
                analysis.getCompletedAt());
    }

    @Test
    void failDoesNotOverwriteCompletedAnalysis() {
        Analysis analysis = completedAnalysis();

        UUID originalAttemptId = analysis.getProcessingAttemptId();
        String originalModelName = analysis.getModelName();
        String originalModelVersion = analysis.getModelVersion();
        String originalResultObjectKey = analysis.getResultObjectKey();
        var originalCompletedAt = analysis.getCompletedAt();

        analysis.fail(
                "different-model",
                "different-version",
                FAILURE_MESSAGE);

        assertEquals(AnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(
                originalAttemptId,
                analysis.getProcessingAttemptId());
        assertEquals(originalModelName, analysis.getModelName());
        assertEquals(
                originalModelVersion,
                analysis.getModelVersion());
        assertEquals(
                originalResultObjectKey,
                analysis.getResultObjectKey());
        assertEquals(
                originalCompletedAt,
                analysis.getCompletedAt());
        assertNull(analysis.getErrorMessage());
    }

    @Test
    void failThrowsWhenAnalysisIsUploaded() {
        Analysis analysis = uploadedAnalysis();

        assertThrows(InvalidAnalysisStateException.class,
                () -> analysis.fail(
                        MODEL_NAME,
                        MODEL_VERSION,
                        FAILURE_MESSAGE));
    }

    private static Analysis uploadedAnalysis() {
        AnalysisInput input = new AnalysisInput(
                "brain-scan.jpg",
                OBJECT_KEY,
                "image/jpeg",
                30310L);

        return Analysis.uploaded(
                ANALYSIS_ID,
                patient(),
                input);
    }

    private static Analysis completedAnalysis() {
        Analysis analysis = queuedAnalysis();

        analysis.complete(
                MODEL_NAME,
                MODEL_VERSION,
                RESULT_OBJECT_KEY,
                detections());

        return analysis;
    }

    private static Analysis failedAnalysis() {
        Analysis analysis = queuedAnalysis();

        analysis.fail(
                MODEL_NAME,
                MODEL_VERSION,
                FAILURE_MESSAGE);

        return analysis;
    }

    private static List<AnalysisDetectionPayload> detections() {
        return List.of(
                new AnalysisDetectionPayload(
                        "glioma",
                        0.92,
                        10,
                        20,
                        100,
                        80));
    }
}
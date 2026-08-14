package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.util.UUID;

public record AnalysisFailedPayload(
        UUID analysisId,
        UUID attemptId,
        String modelName,
        String modelVersion,
        String errorMessage) { }

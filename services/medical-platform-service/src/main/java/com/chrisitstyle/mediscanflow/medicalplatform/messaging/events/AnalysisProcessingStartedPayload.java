package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import java.util.UUID;

public record AnalysisProcessingStartedPayload(
        UUID analysisId,
        UUID attemptId) { }

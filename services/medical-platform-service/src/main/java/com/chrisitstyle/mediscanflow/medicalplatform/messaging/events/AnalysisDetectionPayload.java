package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

public record AnalysisDetectionPayload(
        String label,
        double confidence,
        double x,
        double y,
        double width,
        double height
) {
}

package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisLifecycleService;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisProcessingStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisProcessingStartedEventListener {

    private final AnalysisLifecycleService analysisLifecycleService;

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_PROCESSING_STARTED_QUEUE)
    public void handle(AnalysisProcessingStartedEvent event) {
        analysisLifecycleService.startProcessing(
                event.payload().analysisId(),
                event.payload().attemptId());
    }
}

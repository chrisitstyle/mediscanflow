package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisLifecycleService;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisCompletedEventListener {

    private final AnalysisLifecycleService analysisLifecycleService;

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_COMPLETED_QUEUE)
    public void handle(AnalysisCompletedEvent event) {
        analysisLifecycleService.complete(
                event.payload().analysisId(),
                event.payload().attemptId(),
                event.payload().modelName(),
                event.payload().modelVersion(),
                event.payload().resultObjectKey(),
                event.payload().detections()
        );
    }
}

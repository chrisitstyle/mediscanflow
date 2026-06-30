package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisService;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisCompletedEventListener {

    private final AnalysisService analysisService;

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_COMPLETED_QUEUE)
    public void handle(AnalysisCompletedEvent event) {
        analysisService.complete(
                event.payload().analysisId(),
                event.payload().modelName(),
                event.payload().modelVersion(),
                event.payload().detections()
        );
    }
}

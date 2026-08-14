package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisLifecycleService;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisFailedEventListener {

    private final AnalysisLifecycleService analysisLifecycleService;

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_FAILED_QUEUE)
    public void handle(AnalysisFailedEvent event) {
        analysisLifecycleService.fail(
                event.payload().analysisId(),
                event.payload().attemptId(),
                event.payload().modelName(),
                event.payload().modelVersion(),
                event.payload().errorMessage());
    }
}

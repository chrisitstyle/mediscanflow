package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishAnalysisRequested(AnalysisRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYSIS_EXCHANGE,
                RabbitMQConfig.ANALYSIS_REQUESTED_ROUTING_KEY,
                event
        );
    }
}

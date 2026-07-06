package com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox;

import com.chrisitstyle.mediscanflow.medicalplatform.messaging.AnalysisEventPublisher;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final AnalysisEventPublisher analysisEventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        if (events.isEmpty()) {
            return;
        }

        log.info("Publishing {} pending outbox event(s)", events.size());

        events.forEach(this::publish);
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            if (OutboxEventService.ANALYSIS_REQUESTED_EVENT.equals(outboxEvent.getEventType())) {
                publishAnalysisRequestedEvent(outboxEvent);
                outboxEvent.markPublished();

                log.info(
                        "Published outbox event id={} type={} aggregateId={}",
                        outboxEvent.getId(),
                        outboxEvent.getEventType(),
                        outboxEvent.getAggregateId()
                );

                return;
            }

            String errorMessage = "Unsupported outbox event type: " + outboxEvent.getEventType();
            outboxEvent.markFailed(errorMessage);

            log.warn(
                    "Failed outbox event id={} type={} aggregateId={}: {}",
                    outboxEvent.getId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getAggregateId(),
                    errorMessage
            );
        } catch (RuntimeException exception) {
            markFailedOrRetry(outboxEvent, exception);
        }
    }

    private void publishAnalysisRequestedEvent(OutboxEvent outboxEvent) {
        AnalysisRequestedEvent event = readPayload(
                outboxEvent,
                AnalysisRequestedEvent.class
        );

        analysisEventPublisher.publishAnalysisRequested(event);
    }

    private <T> T readPayload(OutboxEvent outboxEvent, Class<T> payloadType) {
        try {
            return objectMapper.readValue(outboxEvent.getPayload(), payloadType);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not deserialize outbox event payload: " + outboxEvent.getId(),
                    exception
            );
        }
    }

    private void markFailedOrRetry(OutboxEvent outboxEvent, RuntimeException exception) {
        outboxEvent.markFailed(exception.getMessage());

        log.warn(
                "Failed to publish outbox event id={} type={} aggregateId={}; attempts={}/{}",
                outboxEvent.getId(),
                outboxEvent.getEventType(),
                outboxEvent.getAggregateId(),
                outboxEvent.getAttempts(),
                MAX_ATTEMPTS,
                exception
        );

        if (outboxEvent.getAttempts() < MAX_ATTEMPTS) {
            outboxEvent.scheduleRetry();
        }
    }
}
package com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox;

import com.chrisitstyle.mediscanflow.medicalplatform.messaging.AnalysisEventPublisher;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxEventPublisherTest {

    private static final String PAYLOAD = """
            {
              "analysisId": "4ce0289a-2c6e-4fa1-8941-bac2cdf3bd24"
            }
            """;

    private static final UUID ATTEMPT_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");

    private OutboxEventRepository outboxEventRepository;
    private AnalysisEventPublisher analysisEventPublisher;
    private ObjectMapper objectMapper;
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        analysisEventPublisher = mock(AnalysisEventPublisher.class);
        objectMapper = mock(ObjectMapper.class);

        outboxEventPublisher = new OutboxEventPublisher(
                outboxEventRepository,
                analysisEventPublisher,
                objectMapper
        );
    }

    @Test
    void publishPendingEventsPublishesAnalysisRequestedEventAndMarksItAsPublished()
            throws JacksonException {
        OutboxEvent outboxEvent = analysisRequestedOutboxEvent();
        AnalysisRequestedEvent event = analysisRequestedEvent();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(PAYLOAD, AnalysisRequestedEvent.class))
                .thenReturn(event);

        outboxEventPublisher.publishPendingEvents();

        verify(analysisEventPublisher).publishAnalysisRequested(event);

        assertEquals(OutboxEventStatus.PUBLISHED, outboxEvent.getStatus());
        assertEquals(0, outboxEvent.getAttempts());
        assertNull(outboxEvent.getLastError());
    }

    @Test
    void publishPendingEventsSchedulesRetryWhenPublishingFails() throws JacksonException {
        OutboxEvent outboxEvent = analysisRequestedOutboxEvent();
        AnalysisRequestedEvent event = analysisRequestedEvent();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(PAYLOAD, AnalysisRequestedEvent.class))
                .thenReturn(event);

        doThrow(new IllegalStateException("RabbitMQ unavailable"))
                .when(analysisEventPublisher)
                .publishAnalysisRequested(event);

        outboxEventPublisher.publishPendingEvents();

        assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttempts());
        assertEquals("RabbitMQ unavailable", outboxEvent.getLastError());
    }

    @Test
    void publishPendingEventsMarksEventAsFailedAfterMaxAttempts() throws JacksonException {
        OutboxEvent outboxEvent = analysisRequestedOutboxEventWithAttempts(4);
        AnalysisRequestedEvent event = analysisRequestedEvent();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(PAYLOAD, AnalysisRequestedEvent.class))
                .thenReturn(event);

        doThrow(new IllegalStateException("RabbitMQ unavailable"))
                .when(analysisEventPublisher)
                .publishAnalysisRequested(event);

        outboxEventPublisher.publishPendingEvents();

        assertEquals(OutboxEventStatus.FAILED, outboxEvent.getStatus());
        assertEquals(5, outboxEvent.getAttempts());
        assertEquals("RabbitMQ unavailable", outboxEvent.getLastError());
    }

    @Test
    void publishPendingEventsMarksEventAsFailedWhenPayloadCannotBeDeserialized()
            throws JacksonException {
        OutboxEvent outboxEvent = analysisRequestedOutboxEvent();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(PAYLOAD, AnalysisRequestedEvent.class))
                .thenThrow(new JacksonException("Invalid payload") {
                });

        outboxEventPublisher.publishPendingEvents();

        verify(analysisEventPublisher, never())
                .publishAnalysisRequested(org.mockito.ArgumentMatchers.any());

        assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttempts());
        assertTrue(outboxEvent.getLastError().contains("Could not deserialize outbox event payload"));
    }

    @Test
    void publishPendingEventsMarksUnsupportedEventTypeAsFailed() {
        OutboxEvent outboxEvent = OutboxEvent.pending(
                "UNSUPPORTED_EVENT",
                ANALYSIS_ID,
                PAYLOAD
        );

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(outboxEvent));

        outboxEventPublisher.publishPendingEvents();

        verify(analysisEventPublisher, never())
                .publishAnalysisRequested(org.mockito.ArgumentMatchers.any());

        assertEquals(OutboxEventStatus.FAILED, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttempts());
        assertEquals(
                "Unsupported outbox event type: UNSUPPORTED_EVENT",
                outboxEvent.getLastError()
        );
    }

    private static OutboxEvent analysisRequestedOutboxEvent() {
        return OutboxEvent.pending(
                OutboxEventService.ANALYSIS_REQUESTED_EVENT,
                ANALYSIS_ID,
                PAYLOAD
        );
    }

    private static OutboxEvent analysisRequestedOutboxEventWithAttempts(int attempts) {
        OutboxEvent outboxEvent = analysisRequestedOutboxEvent();

        for (int index = 0; index < attempts; index++) {
            outboxEvent.markFailed("Previous failure");
            outboxEvent.scheduleRetry();
        }

        return outboxEvent;
    }

    private static AnalysisRequestedEvent analysisRequestedEvent() {
        return AnalysisRequestedEvent.create(
                ANALYSIS_ID,
                PATIENT_ID,
                OBJECT_KEY,
                ATTEMPT_ID);
    }
}
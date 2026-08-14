package com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisTestEntity.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxEventServiceTest {

    private OutboxEventRepository outboxEventRepository;
    private ObjectMapper objectMapper;
    private OutboxEventService outboxEventService;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        objectMapper = new ObjectMapper();

        outboxEventService = new OutboxEventService(
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void saveAnalysisRequestedEventStoresPendingOutboxEvent() throws JacksonException {
        Analysis analysis = failedAnalysis();

        outboxEventService.saveAnalysisRequestedEvent(analysis);

        ArgumentCaptor<OutboxEvent> outboxEventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(outboxEventCaptor.capture());

        OutboxEvent outboxEvent = outboxEventCaptor.getValue();

        AnalysisRequestedEvent event = objectMapper.readValue(
                outboxEvent.getPayload(),
                AnalysisRequestedEvent.class
        );

        assertEquals(OutboxEventService.ANALYSIS_REQUESTED_EVENT, outboxEvent.getEventType());
        assertEquals(ANALYSIS_ID, outboxEvent.getAggregateId());
        assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus());
        assertEquals(0, outboxEvent.getAttempts());

        assertEquals("AnalysisRequested", event.eventType());
        assertEquals(2, event.eventVersion());
        assertEquals(ANALYSIS_ID, event.payload().analysisId());
        assertEquals(PATIENT_ID, event.payload().patientId());
        assertEquals(OBJECT_KEY, event.payload().objectKey());
    }

    @Test
    void saveAnalysisRequestedEventThrowsWhenPayloadCannotBeSerialized()
            throws JacksonException {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);

        OutboxEventService service = new OutboxEventService(
                outboxEventRepository,
                failingObjectMapper
        );

        Analysis analysis = failedAnalysis();

        when(failingObjectMapper.writeValueAsString(any(AnalysisRequestedEvent.class)))
                .thenThrow(new JacksonException("Serialization failed") {
                });

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.saveAnalysisRequestedEvent(analysis)
        );

        assertEquals(
                "Could not serialize outbox event payload",
                exception.getMessage()
        );

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}
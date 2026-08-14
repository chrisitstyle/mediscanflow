package com.chrisitstyle.mediscanflow.medicalplatform.messaging.outbox;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.Analysis;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisRequestedEvent;
import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    public static final String ANALYSIS_REQUESTED_EVENT = "ANALYSIS_REQUESTED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveAnalysisRequestedEvent(Analysis analysis) {
        AnalysisRequestedEvent event = AnalysisRequestedEvent.create(
                analysis.getId(),
                analysis.getPatient().getId(),
                analysis.getObjectKey());

        OutboxEvent outboxEvent = OutboxEvent.pending(
                ANALYSIS_REQUESTED_EVENT,
                analysis.getId(),
                toJson(event)
        );

        outboxEventRepository.save(outboxEvent);
    }

    private String toJson(AnalysisRequestedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize outbox event payload", exception);
        }
    }
}
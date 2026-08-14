package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisLifecycleService;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisProcessingStartedEvent;
import com.chrisitstyle.mediscanflow.medicalplatform.messaging.events.AnalysisProcessingStartedPayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalysisProcessingStartedEventListenerTest {

    @Test
    void handlesProcessingStartedEvent() {
        AnalysisLifecycleService analysisLifecycleService = mock(AnalysisLifecycleService.class);

        AnalysisProcessingStartedEventListener listener = new AnalysisProcessingStartedEventListener(
                        analysisLifecycleService);

        UUID analysisId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        AnalysisProcessingStartedEvent event = new AnalysisProcessingStartedEvent(
                        UUID.randomUUID(),
                        AnalysisProcessingStartedEvent.TYPE,
                        AnalysisProcessingStartedEvent.VERSION,
                        Instant.now(),
                        UUID.randomUUID(),

                        new AnalysisProcessingStartedPayload(
                                analysisId,
                                attemptId));

        listener.handle(event);

        verify(analysisLifecycleService).startProcessing(
                analysisId,
                attemptId);
    }
}

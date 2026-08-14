package com.chrisitstyle.mediscanflow.medicalplatform.messaging.events;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisMessagingContractTest {

    private static final String ANALYSIS_REQUESTED_SCHEMA = "messaging/analysis-requested.schema.json";

    private static final String ANALYSIS_COMPLETED_SCHEMA = "messaging/analysis-completed.schema.json";

    private static final String ANALYSIS_FAILED_SCHEMA = "messaging/analysis-failed.schema.json";

    private static final String MODEL_NAME = "yolo-brain-tumor-detector";

    private static final String MODEL_VERSION = "yolov8n";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analysisRequestedEventMatchesSchema() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        AnalysisRequestedEvent event = AnalysisRequestedEvent.create(
                analysisId,
                patientId,
                "analyses/%s/brain-scan.jpg".formatted(analysisId),
                attemptId
        );

        assertMatchesSchema(
                event,
                ANALYSIS_REQUESTED_SCHEMA
        );
    }

    @Test
    void analysisCompletedEventMatchesSchema() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        AnalysisCompletedEvent event = new AnalysisCompletedEvent(
                UUID.randomUUID(),
                AnalysisCompletedEvent.TYPE,
                AnalysisCompletedEvent.VERSION,
                Instant.now(),
                UUID.randomUUID(),

                new AnalysisCompletedPayload(
                        analysisId,
                        attemptId,
                        MODEL_NAME,
                        MODEL_VERSION,
                        "analyses/%s/result.jpg".formatted(analysisId),
                        List.of()
                )
        );

        assertMatchesSchema(
                event,
                ANALYSIS_COMPLETED_SCHEMA
        );
    }

    @Test
    void analysisFailedEventMatchesSchema() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        AnalysisFailedEvent event = new AnalysisFailedEvent(
                UUID.randomUUID(),
                AnalysisFailedEvent.TYPE,
                AnalysisFailedEvent.VERSION,
                Instant.now(),
                UUID.randomUUID(),
                new AnalysisFailedPayload(
                        analysisId,
                        attemptId,
                        MODEL_NAME,
                        MODEL_VERSION,
                        "Model inference failed"
                )
        );

        assertMatchesSchema(
                event,
                ANALYSIS_FAILED_SCHEMA
        );
    }

    private void assertMatchesSchema(
            Object event,
            String schemaResource
    ) throws Exception {
        String schemaJson = loadSchema(schemaResource);
        String eventJson = objectMapper.writeValueAsString(event);

        SchemaRegistry schemaRegistry =
                SchemaRegistry.withDialect(
                        Dialects.getDraft202012()
                );

        Schema schema = schemaRegistry.getSchema(
                schemaJson,
                InputFormat.JSON
        );

        List<Error> errors = schema.validate(
                eventJson,
                InputFormat.JSON,
                executionContext ->
                        executionContext.executionConfig(
                                executionConfig ->
                                        executionConfig
                                                .formatAssertionsEnabled(true)
                        )
        );

        assertTrue(
                errors.isEmpty(),
                () -> """
                        Event does not match schema: %s
                        Validation errors: %s
                        Event JSON: %s
                        """.formatted(
                        schemaResource,
                        errors,
                        eventJson
                )
        );
    }

    private String loadSchema(
            String schemaResource
    ) throws Exception {
        try (InputStream inputStream =
                     getClass()
                             .getClassLoader()
                             .getResourceAsStream(schemaResource)) {

            assertNotNull(
                    inputStream,
                    () -> "Schema not found: " + schemaResource
            );

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}

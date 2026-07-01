package com.chrisitstyle.mediscanflow.medicalplatform.system;

import com.chrisitstyle.mediscanflow.medicalplatform.messaging.RabbitMQConfig;
import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemComponentStatusDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemStatusResponseDTO;
import io.minio.MinioClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
class SystemStatusService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final MinioClient minioClient;

    SystemStatusService(
            JdbcTemplate jdbcTemplate,
            RabbitTemplate rabbitTemplate,
            @Qualifier("internalMinioClient") MinioClient minioClient
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.minioClient = minioClient;
    }

    SystemStatusResponseDTO getStatus() {
        Map<String, SystemComponentStatusDTO> components = new LinkedHashMap<>();

        components.put("database", check(this::isDatabaseUp));
        components.put("rabbitmq", check(this::isRabbitMqUp));
        components.put("minio", check(this::isMinioUp));
        components.put("aiWorker", check(this::isAiWorkerUp));

        String overallStatus = components.values().stream()
                .allMatch(component -> UP.equals(component.status()))
                ? UP
                : DOWN;

        return new SystemStatusResponseDTO(overallStatus, components);
    }

    private SystemComponentStatusDTO check(HealthCheck healthCheck) {
        try {
            return new SystemComponentStatusDTO(healthCheck.isUp() ? UP : DOWN);
        } catch (Exception _) {
            return new SystemComponentStatusDTO(DOWN);
        }
    }

    private boolean isDatabaseUp() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1;
    }

    private boolean isRabbitMqUp() {
        return Boolean.TRUE.equals(
                rabbitTemplate.execute(channel -> {
                    channel.queueDeclarePassive(RabbitMQConfig.ANALYSIS_REQUESTED_QUEUE);
                    return true;
                })
        );
    }

    private boolean isMinioUp() throws Exception {
        minioClient.listBuckets();
        return true;
    }

    private boolean isAiWorkerUp() {
        Integer consumerCount = rabbitTemplate.execute(channel ->
                channel.queueDeclarePassive(RabbitMQConfig.ANALYSIS_REQUESTED_QUEUE)
                        .getConsumerCount()
        );

        return consumerCount != null && consumerCount > 0;
    }

    @FunctionalInterface
    private interface HealthCheck {
        boolean isUp() throws Exception;
    }
}

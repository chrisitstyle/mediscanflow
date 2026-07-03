package com.chrisitstyle.mediscanflow.medicalplatform.system;

import com.chrisitstyle.mediscanflow.medicalplatform.messaging.RabbitMQConfig;
import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemComponentStatusDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemStatusResponseDTO;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

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

    private SystemComponentStatusDTO check(BooleanSupplier healthCheck) {
        return new SystemComponentStatusDTO(healthCheck.getAsBoolean() ? UP : DOWN);
    }

    private boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result);
        } catch (DataAccessException _) {
            return false;
        }
    }

    private boolean isRabbitMqUp() {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive(RabbitMQConfig.ANALYSIS_REQUESTED_QUEUE);
                return null;
            });

            return true;
        } catch (AmqpException _) {
            return false;
        }
    }

    private boolean isMinioUp() {
        try {
            minioClient.listBuckets();
            return true;
        } catch (MinioException _) {
            return false;
        }
    }

    private boolean isAiWorkerUp() {
        try {
            Integer consumerCount = rabbitTemplate.execute(channel ->
                    channel.queueDeclarePassive(RabbitMQConfig.ANALYSIS_REQUESTED_QUEUE)
                            .getConsumerCount()
            );

            return consumerCount > 0;
        } catch (AmqpException _) {
            return false;
        }
    }
}
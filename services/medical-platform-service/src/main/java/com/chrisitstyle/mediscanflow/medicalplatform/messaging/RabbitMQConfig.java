package com.chrisitstyle.mediscanflow.medicalplatform.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ANALYSIS_EXCHANGE = "mediscanflow.analysis";
    public static final String ANALYSIS_REQUESTED_QUEUE = "analysis.requested";
    public static final String ANALYSIS_REQUESTED_ROUTING_KEY = "analysis.requested";
    public static final String ANALYSIS_COMPLETED_QUEUE = "analysis.completed";
    public static final String ANALYSIS_COMPLETED_ROUTING_KEY = "analysis.completed";

    @Bean
    DirectExchange analysisExchange() {
        return ExchangeBuilder
                .directExchange(ANALYSIS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    Queue analysisRequestedQueue() {
        return QueueBuilder
                .durable(ANALYSIS_REQUESTED_QUEUE)
                .build();
    }

    @Bean
    Binding analysisRequestedBinding(
            Queue analysisRequestedQueue,
            DirectExchange analysisExchange
    ) {
        return BindingBuilder
                .bind(analysisRequestedQueue)
                .to(analysisExchange)
                .with(ANALYSIS_REQUESTED_ROUTING_KEY);
    }

    @Bean
    Queue analysisCompletedQueue() {
        return QueueBuilder
                .durable(ANALYSIS_COMPLETED_QUEUE)
                .build();
    }

    @Bean
    Binding analysisCompletedBinding(
            Queue analysisCompletedQueue,
            DirectExchange analysisExchange
    ) {
        return BindingBuilder
                .bind(analysisCompletedQueue)
                .to(analysisExchange)
                .with(ANALYSIS_COMPLETED_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}

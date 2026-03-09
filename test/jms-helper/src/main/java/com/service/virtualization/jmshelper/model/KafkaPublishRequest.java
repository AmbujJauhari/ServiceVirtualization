package com.service.virtualization.jmshelper.model;

/**
 * Request body for POST /kafka/publish.
 *
 * @param topic            Kafka topic to publish to
 * @param key              optional message key (null → no key)
 * @param message          message value (string)
 * @param bootstrapServers optional Kafka bootstrap servers (default: localhost:9092)
 */
public record KafkaPublishRequest(
        String topic,
        String key,
        String message,
        String bootstrapServers
) {}

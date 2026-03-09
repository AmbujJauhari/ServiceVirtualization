package com.service.virtualization.jmshelper.model;

/**
 * Request body for POST /kafka/consume.
 *
 * @param topic            Kafka topic to consume from
 * @param timeoutMs        how long to wait for a message (default: 6000ms)
 * @param bootstrapServers optional Kafka bootstrap servers (default: localhost:9092)
 */
public record KafkaConsumeRequest(
        String topic,
        Integer timeoutMs,
        String bootstrapServers
) {
    public long effectiveTimeout() {
        return timeoutMs != null ? timeoutMs : 6_000L;
    }

    public String effectiveBootstrapServers() {
        return bootstrapServers != null && !bootstrapServers.isBlank()
                ? bootstrapServers
                : "localhost:9092";
    }
}

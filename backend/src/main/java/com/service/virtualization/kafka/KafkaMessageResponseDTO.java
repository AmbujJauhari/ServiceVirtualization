package com.service.virtualization.kafka;

/**
 * DTO for Kafka message operation response
 */
public record KafkaMessageResponseDTO(
    boolean success,
    String message
) {
} 
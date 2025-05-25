package com.service.virtualization.kafka.dto;

/**
 * DTO for Kafka message operation response
 */
public record KafkaMessageResponseDTO(
    boolean success,
    String message
) {
} 
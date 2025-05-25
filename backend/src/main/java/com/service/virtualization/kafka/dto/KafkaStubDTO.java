package com.service.virtualization.kafka.dto;

import java.time.LocalDateTime;

public record KafkaStubDTO(
    Long id,
    String name,
    String description,
    String userId,
    String topic,
    String keyPattern,
    String valuePattern,
    Boolean activeForProducer,
    Boolean activeForConsumer,
    String responseType,
    String responseContent,
    Integer latency,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 
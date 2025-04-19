package com.service.virtualization.kafka;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Kafka Stubs
 */
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
    LocalDateTime updatedAt,
    Map<String, String> matchCriteria,
    Map<String, String> headerMatchCriteria,
    List<String> tags
) {
    /**
     * DTO for body match criteria
     */
    public record BodyMatchCriteriaDTO(
        String type,  // "xpath", "jsonpath"
        String expression,
        String value,
        String operator  // "equals", "contains", "startsWith", "endsWith", "regex"
    ) {}
} 
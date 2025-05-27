package com.service.virtualization.kafka.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record KafkaStubDTO(
    String id,
    String name,
    String description,
    String userId,
    String requestTopic,
    String responseTopic,
    
    // Content formats and matching
    String requestContentFormat,
    String responseContentFormat,
    String requestContentMatcher,
    
    // Key matching
    String keyMatchType,
    String keyPattern,
    
    // Content/Value matching
    String contentMatchType,
    String valuePattern, // Legacy field - keeping for compatibility
    String contentPattern, // New field name
    Boolean caseSensitive,
    
    // Response configuration
    String responseType,
    String responseKey,
    String responseContent,
    
    // Schema Registry for response validation
    Boolean useResponseSchemaRegistry,
    String responseSchemaId,
    String responseSchemaSubject,
    String responseSchemaVersion,
    
    Integer latency,
    
    // Callback configuration
    String callbackUrl,
    Map<String, String> callbackHeaders,
    
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 
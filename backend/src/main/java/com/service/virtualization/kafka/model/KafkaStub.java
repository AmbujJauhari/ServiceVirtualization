package com.service.virtualization.kafka.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.service.virtualization.model.StubStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain model for Kafka stubs.
 * Supports multi-server Kafka configuration.
 */
@Document(collection = "kafka_stubs")
public record KafkaStub(
    @Id String id,
    String name,
    String description,
    String userId,
    
    // Multi-server support
    String serverName,         // Which Kafka cluster to listen on
    String responseServerName, // Which Kafka cluster to respond on (null = same as serverName)
    
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
    String valuePattern, // Legacy field
    String contentPattern, // New field name
    Boolean caseSensitive,
    
    // Response configuration
    String responseType, // 'direct' or 'callback'
    String responseKey,
    String responseContent,
    
    // Schema Registry for response validation (UI-only, not persisted)
    Boolean useResponseSchemaRegistry,
    String responseSchemaId,
    String responseSchemaSubject,
    String responseSchemaVersion,
    
    Integer latency,
    StubStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Callback configuration
    String callbackUrl,
    Map<String, String> callbackHeaders,

    List<String> tags
) {
    /**
     * Get the effective server name to respond on.
     * If responseServerName is set, use it; otherwise use serverName.
     */
    public String getEffectiveResponseServerName() {
        return (responseServerName != null && !responseServerName.trim().isEmpty()) 
            ? responseServerName 
            : serverName;
    }

    /**
     * Default constructor with sensible defaults
     */
    public KafkaStub() {
        this(
            null, // id - Let database auto-generate ID
            null, // name
            null, // description
            null, // userId
            null, // serverName
            null, // responseServerName
            null, // requestTopic
            null, // responseTopic
            null, // requestContentFormat
            null, // responseContentFormat
            null, // requestContentMatcher
            null, // keyMatchType
            null, // keyPattern
            null, // contentMatchType
            null, // valuePattern
            null, // contentPattern
            false, // caseSensitive
            null, // responseType
            null, // responseKey
            null, // responseContent
            null, // useResponseSchemaRegistry
            null, // responseSchemaId
            null, // responseSchemaSubject
            null, // responseSchemaVersion
            null, // latency
            StubStatus.INACTIVE, // status
            LocalDateTime.now(), // createdAt
            LocalDateTime.now(), // updatedAt
            null, // callbackUrl
            null, // callbackHeaders
            List.of() // tags
        );
    }
} 
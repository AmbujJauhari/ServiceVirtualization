package com.service.virtualization.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a recording of a service interaction.
 * This model is protocol-agnostic and can store any protocol-specific data.
 */
public record Recording(
    // Base virtualization item fields
    String id,
    String name,
    String description,
    String userId,
    boolean behindProxy,
    Protocol protocol,
    Map<String, Object> protocolData,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    
    // Recording-specific fields
    String sessionId,
    LocalDateTime recordedAt,
    boolean convertedToStub,
    String convertedStubId,
    String sourceIp,
    Map<String, Object> requestData,
    Map<String, Object> responseData
) {
    /**
     * Default constructor with defaults for all fields
     */
    public Recording() {
        this(
            UUID.randomUUID().toString(),
            null,
            null,
            null,
            false,
            Protocol.getDefault(),
            new HashMap<>(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            LocalDateTime.now(),
            false,
            null,
            null,
            new HashMap<>(),
            new HashMap<>()
        );
    }
    
    /**
     * Canonical constructor with validation
     */
    public Recording {
        // No default ID generation - let database handle it
        
        if (protocol == null) {
            protocol = Protocol.getDefault();
        }
        if (protocolData == null) {
            protocolData = new HashMap<>();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
        if (requestData == null) {
            requestData = new HashMap<>();
        }
        if (responseData == null) {
            responseData = new HashMap<>();
        }
    }
    

    /**
     * Validates if the recording has all required fields populated
     * @return true if the recording is valid, false otherwise
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
               && protocol != null
               && userId != null && !userId.trim().isEmpty()
               && sessionId != null && !sessionId.trim().isEmpty()
               && !requestData.isEmpty()
               && !responseData.isEmpty();
    }
    
    /**
     * Gets a new recording object initialized with default values
     * @return a new recording with default values
     */
    public static Recording getDefault() {
        return new Recording();
    }
} 
package com.service.virtualization.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a service virtualization stub that can be used to simulate service behavior.
 * This model is protocol-agnostic and can store any protocol-specific data.
 */
public record Stub(
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
    
    // Stub-specific fields
    String wiremockMappingId,
    StubStatus status,
    Map<String, Object> requestData,
    Map<String, Object> responseData
) {
    /**
     * Default constructor with defaults for all fields
     */
    public Stub() {
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
            StubStatus.INACTIVE,
            new HashMap<>(),
            new HashMap<>()
        );
    }
    
    /**
     * Canonical constructor with validation
     */
    public Stub {
        // Ensure defaults for null values
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
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
        if (status == null) {
            status = StubStatus.INACTIVE;
        }
        if (requestData == null) {
            requestData = new HashMap<>();
        }
        if (responseData == null) {
            responseData = new HashMap<>();
        }
    }
    /**
     * Create a new Stub with the updated request data
     */
    public Stub withRequestData(Map<String, Object> newRequestData) {
        return new Stub(
            id, name, description, userId, behindProxy, protocol, protocolData,
            createdAt, LocalDateTime.now(), wiremockMappingId, status, 
            newRequestData, responseData
        );
    }
    
    /**
     * Create a new Stub with the updated response data
     */
    public Stub withResponseData(Map<String, Object> newResponseData) {
        return new Stub(
            id, name, description, userId, behindProxy, protocol, protocolData,
            createdAt, LocalDateTime.now(), wiremockMappingId, status, 
            requestData, newResponseData
        );
    }
    
    /**
     * Create a new Stub with the updated status
     */
    public Stub withStatus(StubStatus newStatus) {
        return new Stub(
            id, name, description, userId, behindProxy, protocol, protocolData,
            createdAt, LocalDateTime.now(), wiremockMappingId, newStatus, 
            requestData, responseData
        );
    }
    
    /**
     * Create a new Stub with the updated WireMock mapping ID
     */
    public Stub withWiremockMappingId(String newWiremockMappingId) {
        return new Stub(
            id, name, description, userId, behindProxy, protocol, protocolData,
            createdAt, LocalDateTime.now(), newWiremockMappingId, status, 
            requestData, responseData
        );
    }
    
    /**
     * Validates if the stub has all required fields populated
     * @return true if the stub is valid, false otherwise
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() 
               && protocol != null
               && userId != null && !userId.trim().isEmpty()
               && !requestData.isEmpty() 
               && !responseData.isEmpty();
    }
    
    /**
     * Gets a new stub object initialized with default values
     * @return a new stub with default values
     */
    public static Stub getDefault() {
        return new Stub();
    }
} 
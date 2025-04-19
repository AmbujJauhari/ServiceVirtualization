package com.service.virtualization.dto;

import com.service.virtualization.model.MessageHeader;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for ActiveMQStub
 */
public record ActiveMQStubDTO(
    String id,
    String name,
    String description,
    String userId,
    String destinationType,
    String destinationName,
    String selector,
    String responseContent,
    String responseType,
    Integer latency,
    List<MessageHeader> headers,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Default constructor with empty values
     */
    public ActiveMQStubDTO() {
        this(null, null, null, null, null, null, null, null, null, 0, List.of(), "INACTIVE", null, null);
    }
} 
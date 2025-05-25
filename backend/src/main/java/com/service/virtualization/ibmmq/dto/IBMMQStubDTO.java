package com.service.virtualization.ibmmq.dto;

import com.service.virtualization.model.MessageHeader;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for IBMMQStub
 */
public record IBMMQStubDTO(
    String id,
    String name,
    String description,
    String userId,
    String queueManager,
    String queueName,
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
    public IBMMQStubDTO() {
        this(null, null, null, null, null, null, null, null, null, 0, List.of(), "INACTIVE", null, null);
    }
} 
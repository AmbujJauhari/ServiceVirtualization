package com.service.virtualization.ibmmq.dto;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.model.StubStatus;

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
    
    // Standardized content matching configuration
    IBMMQStub.ContentMatchType contentMatchType,
    String contentPattern,
    Boolean caseSensitive,
    Integer priority,
    
    String responseContent,
    String responseType,
    String responseDestination,
    String responseDestinationType,
    String webhookUrl,
    Integer latency,
    List<MessageHeader> headers,
    StubStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Default constructor with empty values
     */
    public IBMMQStubDTO() {
        this(null, null, null, null, null, null, null, 
             IBMMQStub.ContentMatchType.NONE, null, false, 0, 
             null, null, null, "queue", null, 0, List.of(), StubStatus.INACTIVE, null, null);
    }
} 
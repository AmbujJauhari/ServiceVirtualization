package com.service.virtualization.tibco.dto;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.tibco.model.TibcoStub;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for TIBCO EMS message stubs
 */
public record TibcoStubDTO(
    String id,
    String name,
    String description,
    String userId,
    TibcoDestinationDTO requestDestination,
    TibcoDestinationDTO responseDestination,
    String messageSelector,
    
    // Legacy body match criteria (kept for backward compatibility)
    List<BodyMatchCriteriaDTO> bodyMatchCriteria,
    
    // Standardized content matching configuration
    TibcoStub.ContentMatchType contentMatchType,
    String contentPattern,
    Boolean caseSensitive,
    
    // Priority for stub matching
    Integer priority,
    
    String responseType,  // DIRECT or CALLBACK
    String responseContent,
    Map<String, String> responseHeaders,
    
    // Response latency in milliseconds
    Long latency,
    
    StubStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * DTO for the TIBCO EMS destination configuration.
     */
    public record TibcoDestinationDTO(
        String type,  // QUEUE or TOPIC
        String name
    ) {}
    
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
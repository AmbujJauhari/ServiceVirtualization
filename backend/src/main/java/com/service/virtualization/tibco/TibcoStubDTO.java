package com.service.virtualization.tibco;

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
    List<BodyMatchCriteriaDTO> bodyMatchCriteria,
    String responseType,  // DIRECT or CALLBACK
    String responseContent,
    Map<String, String> responseHeaders,
    String status,
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
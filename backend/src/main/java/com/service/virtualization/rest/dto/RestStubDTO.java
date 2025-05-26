package com.service.virtualization.rest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Stub with nested structure matching frontend expectations
 */
public record RestStubDTO(
    String id,
    String name,
    String description,
    String userId,
    boolean behindProxy,
    String protocol,
    List<String> tags,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String wiremockMappingId,
    Map<String, Object> matchConditions,
    Map<String, Object> response,
    String webhookUrl

) {

} 
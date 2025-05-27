package com.service.virtualization.rest.model;

import com.service.virtualization.model.Protocol;
import com.service.virtualization.model.StubStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Transfer Object for Stub with nested structure matching frontend expectations
 */
@Document(collection = "rest_stubs")
public record RestStub(
        @Id String id,
        String name,
        String description,
        String userId,
        boolean behindProxy,
        String protocol,
        List<String> tags,
        StubStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String wiremockMappingId,

        Map<String, Object> matchConditions,
        Map<String, Object> response,
        String webhookUrl
) {
    public RestStub() {
        this(
                null, // Let database auto-generate ID
                null,
                null,
                null,
                false,
                Protocol.getDefault().name(),
                new ArrayList<>(),
                StubStatus.INACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                new HashMap<>(),
                new HashMap<>(),
                null
        );
    }

    /**
     * Canonical constructor with validation
     */
    public RestStub {
        // Ensure defaults for null values (but don't generate ID manually)
        if (protocol == null) {
            protocol = Protocol.getDefault().name();
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
        if (matchConditions == null) {
            matchConditions = new HashMap<>();
        }
        if (response == null) {
            response = new HashMap<>();
        }
        // webhookUrl can be null (no webhook)
    }
    /**
     * Create a new Stub with the updated request data
     */
    public RestStub withMatchingConditions(Map<String, Object> matchConditions) {
        return new RestStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), wiremockMappingId, matchConditions, response, webhookUrl
        );
    }

    /**
     * Create a new Stub with the updated response data
     */
    public RestStub withResponse(Map<String, Object> newResponse) {
        return new RestStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), wiremockMappingId, matchConditions, newResponse, webhookUrl
        );

    }

    /**
     * Create a new Stub with the updated status
     */
    public RestStub withStatus(StubStatus newStatus) {
        return new RestStub(
                id, name, description, userId, behindProxy, protocol, tags, newStatus,
                createdAt, LocalDateTime.now(), wiremockMappingId, matchConditions, response, webhookUrl
        );
    }

    /**
     * Create a new Stub with the updated WireMock mapping ID
     */
    public RestStub withWiremockMappingId(String newWiremockMappingId) {
        return new RestStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), newWiremockMappingId, matchConditions, response, webhookUrl
        );
    }

    /**
     * Create a new Stub with the updated webhook URL
     */
    public RestStub withWebhookUrl(String newWebhookUrl) {
        return new RestStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), wiremockMappingId, matchConditions, response, newWebhookUrl
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
                && !matchConditions.isEmpty()
                && !response.isEmpty();
    }

    /**
     * Check if this stub has webhook configured
     * @return true if webhook URL is configured and not empty
     */
    public boolean hasWebhook() {
        return webhookUrl != null && !webhookUrl.trim().isEmpty();
    }

    /**
     * Gets a new stub object initialized with default values
     * @return a new stub with default values
     */
    public static RestStub getDefault() {
        return new RestStub();
    }
}
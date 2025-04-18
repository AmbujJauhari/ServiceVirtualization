package com.service.virtualization.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a recording configuration
 */
public record RecordingConfig(
    String id,
    String name,
    String description,
    String pathPattern,
    boolean active,
    String userId,
    boolean useHttps,
    byte[] certificateData,
    String certificatePassword,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> tags
) {
    public RecordingConfig(
        String id,
        String name,
        String description,
        String pathPattern,
        boolean active,
        String userId,
        boolean useHttps,
        byte[] certificateData,
        String certificatePassword,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this(id, name, description, pathPattern, active, userId, useHttps, certificateData, certificatePassword, createdAt, updatedAt, List.of());
    }

    public RecordingConfig withActive(boolean active) {
        return new RecordingConfig(
            id,
            name,
            description,
            pathPattern,
            active,
            userId,
            useHttps,
            certificateData,
            certificatePassword,
            createdAt,
            updatedAt,
            tags
        );
    }

    public RecordingConfig withTags(List<String> tags) {
        return new RecordingConfig(
            id,
            name,
            description,
            pathPattern,
            active,
            userId,
            useHttps,
            certificateData,
            certificatePassword,
            createdAt,
            updatedAt,
            tags
        );
    }

    public boolean matches(String path) {
        return path.matches(pathPattern);
    }

    public RecordingConfig withHttpsSettings(boolean useHttps, byte[] certificateData, String certificatePassword) {
        return new RecordingConfig(
            id,
            name,
            description,
            pathPattern,
            active,
            userId,
            useHttps,
            certificateData,
            certificatePassword,
            createdAt,
            LocalDateTime.now(),
            tags
        );
    }
} 
package com.service.virtualization.model;

/**
 * Represents the status of a stub
 */
public enum StubStatus {
    ACTIVE,      // Stub is active and ready to serve responses
    INACTIVE,    // Stub is inactive (disabled but not deleted)
    STUB_NOT_REGISTERED, // Stub is not registered with wiremock
    ARCHIVED,    // Stub is archived (not used but kept for reference)
    DRAFT        // Stub is in draft mode (not yet published)
} 
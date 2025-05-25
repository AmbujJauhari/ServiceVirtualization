package com.service.virtualization.tibco.model;

import java.util.Map;

/**
 * Represents a TIBCO EMS message with headers and payload
 */
public record TibcoMessage(
    String id,
    Map<String, String> headers,
    String payload,
    MessageType messageType,
    String correlationId
) {
    
    /**
     * Enum representing the types of TIBCO EMS messages
     */
    public enum MessageType {
        TEXT,
        BYTES,
        MAP,
        OBJECT
    }
} 
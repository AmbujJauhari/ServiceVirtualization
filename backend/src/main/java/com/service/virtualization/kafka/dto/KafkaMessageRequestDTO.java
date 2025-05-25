package com.service.virtualization.kafka.dto;

import java.util.Map;

/**
 * DTO for Kafka message publish request
 */
public record KafkaMessageRequestDTO(
    String topic,
    String key,
    String contentType,
    String message,
    Map<String, String> headers
) {
    /**
     * Get the topic
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Get the key
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the content type
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the message
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the headers
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
} 
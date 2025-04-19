package com.service.virtualization.kafka;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Kafka message operations
 */
public interface KafkaMessageService {
    
    /**
     * Publish a message to a Kafka topic
     *
     * @param topic The Kafka topic
     * @param key The message key (optional)
     * @param content The message content
     * @param headers Additional headers (optional)
     */
    void publishMessage(String topic, String key, String content, Map<String, String> headers);
    
    /**
     * Get list of available Kafka topics
     *
     * @return List of topic names
     */
    List<String> getAvailableTopics();
} 
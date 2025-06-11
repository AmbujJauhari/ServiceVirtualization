package com.service.virtualization.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling Kafka message operations
 * Only active when kafka-disabled profile is NOT active
 */
@Service
@Profile("!kafka-disabled")
public class KafkaMessageService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAdmin kafkaAdmin;
    private final SchemaRegistryService schemaRegistryService;
    
    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // Schema Registry header constants
    private static final String SCHEMA_ID_HEADER = "schema-id";
    private static final String SCHEMA_SUBJECT_HEADER = "schema-subject";
    private static final String SCHEMA_VERSION_HEADER = "schema-version";

    @Autowired
    public KafkaMessageService(KafkaTemplate<String, String> kafkaTemplate, KafkaAdmin kafkaAdmin, SchemaRegistryService schemaRegistryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaAdmin = kafkaAdmin;
        this.schemaRegistryService = schemaRegistryService;
    }

    public void publishMessage(String topic, String key, String content, Map<String, String> headers) {
        logger.info("Publishing message to topic: {}", topic);
        
        // Validate schema registry headers if present
        if (headers != null) {
            validateSchemaRegistryHeaders(headers);
            
            // Perform schema validation if schema information is provided
            if (hasSchemaRegistryHeaders(headers)) {
                String validationError = schemaRegistryService.validateMessage(content, headers);
                if (validationError != null) {
                    throw new IllegalArgumentException("Schema validation failed: " + validationError);
                }
                logger.info("Message validated successfully against schema");
            }
        }
        
        // Create record with headers if provided
        ProducerRecord<String, String> record;
        
        if (headers != null && !headers.isEmpty()) {
            List<Header> kafkaHeaders = headers.entrySet().stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());
                
            record = new ProducerRecord<>(topic, null, key, content, kafkaHeaders);
        } else {
            record = new ProducerRecord<>(topic, key, content);
        }
        
        // Send the message
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to send message to topic: {}", topic, ex);
            } else {
                logger.info("Message sent successfully to topic: {}", topic);
                if (hasSchemaRegistryHeaders(headers)) {
                    logger.info("Message published with schema registry headers");
                }
            }
        });
    }

    /**
     * Validate schema registry headers for consistency
     */
    private void validateSchemaRegistryHeaders(Map<String, String> headers) {
        boolean hasSchemaId = headers.containsKey(SCHEMA_ID_HEADER);
        boolean hasSchemaSubject = headers.containsKey(SCHEMA_SUBJECT_HEADER);
        
        if (hasSchemaId && hasSchemaSubject) {
            logger.warn("Both schema-id and schema-subject provided. Schema-id will take precedence.");
        }
        
        if (hasSchemaId) {
            String schemaId = headers.get(SCHEMA_ID_HEADER);
            try {
                Integer.parseInt(schemaId);
                logger.debug("Using schema registry with schema ID: {}", schemaId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid schema ID format: {}. Should be a numeric value.", schemaId);
            }
        }
        
        if (hasSchemaSubject) {
            String schemaSubject = headers.get(SCHEMA_SUBJECT_HEADER);
            String schemaVersion = headers.getOrDefault(SCHEMA_VERSION_HEADER, "latest");
            logger.debug("Using schema registry with subject: {} and version: {}", schemaSubject, schemaVersion);
        }
    }

    /**
     * Check if headers contain schema registry information
     */
    private boolean hasSchemaRegistryHeaders(Map<String, String> headers) {
        return headers != null && (
            headers.containsKey(SCHEMA_ID_HEADER) || 
            headers.containsKey(SCHEMA_SUBJECT_HEADER)
        );
    }

    public List<String> getAvailableTopics() {
        logger.info("Getting available Kafka topics");
        
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult topics = adminClient.listTopics();
            return new ArrayList<>(topics.names().get());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to get Kafka topics", e);
            Thread.currentThread().interrupt();
            return List.of(); // Return empty list on error
        }
    }
} 
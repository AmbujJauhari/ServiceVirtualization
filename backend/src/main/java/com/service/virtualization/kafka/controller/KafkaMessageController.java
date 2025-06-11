package com.service.virtualization.kafka.controller;

import com.service.virtualization.kafka.service.KafkaMessageService;
import com.service.virtualization.kafka.service.SchemaRegistryService;
import com.service.virtualization.kafka.dto.KafkaMessageRequestDTO;
import com.service.virtualization.kafka.dto.KafkaMessageResponseDTO;
import com.service.virtualization.kafka.dto.SchemaInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Kafka message operations
 * Only active when kafka-disabled profile is NOT active
 */
@RestController
@RequestMapping("/api/kafka")
@Profile("!kafka-disabled")
public class KafkaMessageController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageController.class);

    private final KafkaMessageService kafkaMessageService;
    private final SchemaRegistryService schemaRegistryService;

    @Autowired
    public KafkaMessageController(KafkaMessageService kafkaMessageService, SchemaRegistryService schemaRegistryService) {
        this.kafkaMessageService = kafkaMessageService;
        this.schemaRegistryService = schemaRegistryService;
    }

    /**
     * Publish a message to a Kafka topic
     *
     * @param request The message request containing topic, key, and content
     * @return Success response
     */
    @PostMapping("/publish")
    public ResponseEntity<KafkaMessageResponseDTO> publishMessage(@RequestBody KafkaMessageRequestDTO request) {
        logger.info("Publishing message to topic: {}", request.getTopic());
        
        kafkaMessageService.publishMessage(
            request.getTopic(),
            request.getKey(),
            request.getMessage(),
            request.getHeaders()
        );
        
        return ResponseEntity.ok(new KafkaMessageResponseDTO(
            true,
            "Message published successfully to topic: " + request.getTopic()
        ));
    }

    /**
     * Get available Kafka topics
     *
     * @return List of available Kafka topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getTopics() {
        logger.info("Fetching available Kafka topics");
        
        List<String> topics = kafkaMessageService.getAvailableTopics();
        
        return ResponseEntity.ok(topics);
    }

    /**
     * Get available schemas from Schema Registry
     *
     * @return List of available schemas
     */
    @GetMapping("/schemas")
    public ResponseEntity<List<SchemaInfoDTO>> getAvailableSchemas() {
        logger.info("Fetching available schemas from Schema Registry");
        
        List<SchemaInfoDTO> schemas = schemaRegistryService.getAvailableSchemas();
        
        return ResponseEntity.ok(schemas);
    }

    /**
     * Get versions for a specific schema subject
     *
     * @param subject The schema subject
     * @return List of available versions
     */
    @GetMapping("/schemas/{subject}/versions")
    public ResponseEntity<List<Integer>> getSchemaVersions(@PathVariable String subject) {
        logger.info("Fetching versions for schema subject: {}", subject);
        
        List<Integer> versions = schemaRegistryService.getSchemaVersions(subject);
        
        return ResponseEntity.ok(versions);
    }

    /**
     * Validate message content against a schema
     *
     * @param request The message request with schema information
     * @return Validation result
     */
    @PostMapping("/validate-schema")
    public ResponseEntity<KafkaMessageResponseDTO> validateMessageSchema(@RequestBody KafkaMessageRequestDTO request) {
        logger.info("Validating message against schema");
        
        try {
            String validationResult = schemaRegistryService.validateMessage(
                request.getMessage(),
                request.getHeaders()
            );
            
            if (validationResult == null) {
                return ResponseEntity.ok(new KafkaMessageResponseDTO(true, "Message is valid according to schema"));
            } else {
                return ResponseEntity.badRequest().body(new KafkaMessageResponseDTO(false, validationResult));
            }
        } catch (Exception e) {
            logger.error("Schema validation failed", e);
            return ResponseEntity.badRequest().body(new KafkaMessageResponseDTO(false, "Schema validation error: " + e.getMessage()));
        }
    }
} 
package com.service.virtualization.controller;

import com.service.virtualization.dto.KafkaMessageRequestDTO;
import com.service.virtualization.dto.KafkaMessageResponseDTO;
import com.service.virtualization.service.KafkaMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Kafka message operations
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaMessageController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageController.class);

    private final KafkaMessageService kafkaMessageService;

    @Autowired
    public KafkaMessageController(KafkaMessageService kafkaMessageService) {
        this.kafkaMessageService = kafkaMessageService;
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
} 
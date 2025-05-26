package com.service.virtualization.ibmmq.controller;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.dto.IBMMQStubDTO;
import com.service.virtualization.ibmmq.service.IBMMQStubService;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing IBM MQ stubs
 */
@RestController
@RequestMapping("/api/ibmmq/stubs")
@CrossOrigin(origins = "*")
public class IBMMQStubController {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQStubController.class);

    private final IBMMQStubService ibmMQStubService;

    @Autowired
    public IBMMQStubController(IBMMQStubService ibmMQStubService) {
        this.ibmMQStubService = ibmMQStubService;
    }

    /**
     * Create a new IBM MQ stub
     */
    @PostMapping
    public ResponseEntity<IBMMQStubDTO> createStub(@RequestBody IBMMQStubDTO stubDTO) {
        logger.info("Creating new IBM MQ stub: {}", stubDTO.name());

        // Convert DTO to entity
        IBMMQStub stub = convertToEntity(stubDTO);

        // Save the entity
        IBMMQStub createdStub = ibmMQStubService.create(stub);

        // Convert back to DTO and return
        return new ResponseEntity<>(convertToDTO(createdStub), HttpStatus.CREATED);
    }

    /**
     * Get all IBM MQ stubs
     */
    @GetMapping
    public ResponseEntity<List<IBMMQStubDTO>> getAllStubs() {
        logger.info("Fetching all IBM MQ stubs");

        List<IBMMQStub> stubs = ibmMQStubService.findAll();
        List<IBMMQStubDTO> stubDTOs = stubs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(stubDTOs);
    }

    /**
     * Get IBM MQ stubs by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<IBMMQStubDTO>> getStubsByUser(@PathVariable String userId) {
        logger.info("Fetching IBM MQ stubs for user: {}", userId);

        List<IBMMQStub> stubs = ibmMQStubService.findByUserId(userId);
        List<IBMMQStubDTO> stubDTOs = stubs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(stubDTOs);
    }

    /**
     * Get IBM MQ stubs by user ID and active status
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<IBMMQStubDTO>> getActiveStubsByUser(@PathVariable String userId) {
        logger.info("Fetching active IBM MQ stubs for user: {}", userId);

        List<IBMMQStub> stubs = ibmMQStubService.findActiveByUserId(userId);
        List<IBMMQStubDTO> stubDTOs = stubs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(stubDTOs);
    }

    /**
     * Get a IBM MQ stub by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IBMMQStubDTO> getStubById(@PathVariable String id) {
        logger.info("Fetching IBM MQ stub with ID: {}", id);

        Optional<IBMMQStub> stubOptional = ibmMQStubService.findById(id);

        return stubOptional
                .map(stub -> ResponseEntity.ok(convertToDTO(stub)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing IBM MQ stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<IBMMQStubDTO> updateStub(@PathVariable String id, @RequestBody IBMMQStubDTO stubDTO) {
        logger.info("Updating IBM MQ stub with ID: {}", id);

        // Convert DTO to entity
        IBMMQStub stub = convertToEntity(stubDTO);

        // Update the entity
        IBMMQStub updatedStub = ibmMQStubService.update(id, stub);

        // Convert back to DTO and return
        return ResponseEntity.ok(convertToDTO(updatedStub));
    }

    /**
     * Delete an IBM MQ stub
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStub(@PathVariable String id) {
        logger.info("Deleting IBM MQ stub with ID: {}", id);

        ibmMQStubService.delete(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Update the status of an IBM MQ stub
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<IBMMQStubDTO> updateStubStatus(@PathVariable String id, @RequestBody Map<String, String> statusUpdate) {
        StubStatus status = StubStatus.valueOf(statusUpdate.get("status"));

        if (status == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Updating status of IBM MQ stub with ID: {} to {}", id, status);

        IBMMQStub updatedStub = ibmMQStubService.updateStatus(id, status);

        return ResponseEntity.ok(convertToDTO(updatedStub));
    }

    /**
     * Add a header to an IBM MQ stub
     */
    @PostMapping("/{id}/headers")
    public ResponseEntity<IBMMQStubDTO> addStubHeader(@PathVariable String id, @RequestBody MessageHeader header) {
        logger.info("Adding header to IBM MQ stub with ID: {}", id);

        IBMMQStub updatedStub = ibmMQStubService.addHeader(id, header);

        return ResponseEntity.ok(convertToDTO(updatedStub));
    }

    /**
     * Remove a header from an IBM MQ stub
     */
    @DeleteMapping("/{id}/headers/{headerName}")
    public ResponseEntity<IBMMQStubDTO> removeStubHeader(@PathVariable String id, @PathVariable String headerName) {
        logger.info("Removing header '{}' from IBM MQ stub with ID: {}", headerName, id);

        IBMMQStub updatedStub = ibmMQStubService.removeHeader(id, headerName);

        return ResponseEntity.ok(convertToDTO(updatedStub));
    }

    /**
     * Get IBM MQ stubs by queue name
     */
    @GetMapping("/queue/{queueName}")
    public ResponseEntity<List<IBMMQStubDTO>> getStubsByQueueName(@PathVariable String queueName) {
        logger.info("Fetching IBM MQ stubs for queue: {}", queueName);

        List<IBMMQStub> stubs = ibmMQStubService.findByQueueName(queueName);
        List<IBMMQStubDTO> stubDTOs = stubs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(stubDTOs);
    }

    /**
     * Get IBM MQ stubs by queue manager and queue name
     */
    @GetMapping("/manager/{queueManager}/queue/{queueName}")
    public ResponseEntity<List<IBMMQStubDTO>> getStubsByQueueManagerAndName(
            @PathVariable String queueManager,
            @PathVariable String queueName) {
        logger.info("Fetching IBM MQ stubs for queue manager: {} and queue: {}", queueManager, queueName);

        List<IBMMQStub> stubs = ibmMQStubService.findByQueueManagerAndName(queueManager, queueName);
        List<IBMMQStubDTO> stubDTOs = stubs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(stubDTOs);
    }

    /**
     * Publish a message to an IBM MQ queue
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishMessage(@RequestBody Map<String, Object> messageRequest) {
        try {
            String queueManager = (String) messageRequest.get("queueManager");
            String queueName = (String) messageRequest.get("queueName");
            String message = (String) messageRequest.get("message");
            List<Map<String, String>> headerMaps = (List<Map<String, String>>) messageRequest.getOrDefault("headers", new ArrayList<>());

            // Convert headers from maps to MessageHeader objects
            List<MessageHeader> headers = headerMaps.stream()
                    .map(map -> new MessageHeader(
                            map.get("name"),
                            map.get("value"),
                            map.getOrDefault("type", "string")))
                    .collect(Collectors.toList());

            logger.info("Publishing message to queue '{}' on manager '{}'", queueName, queueManager);

            boolean success = ibmMQStubService.publishMessage(queueManager, queueName, message, headers);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Message published successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "success", false,
                        "message", "Failed to publish message"
                ));
            }
        } catch (Exception e) {
            logger.error("Error publishing message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error publishing message: " + e.getMessage()
            ));
        }
    }

    /**
     * Convert a DTO to an entity
     */
    private IBMMQStub convertToEntity(IBMMQStubDTO dto) {
        IBMMQStub entity = new IBMMQStub();

        if (dto.id() != null) {
            entity.setId(dto.id());
        }

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setUserId(dto.userId());
        entity.setQueueManager(dto.queueManager());
        entity.setQueueName(dto.queueName());
        entity.setSelector(dto.selector());
        
        // Standardized content matching fields
        entity.setContentMatchType(dto.contentMatchType() != null ? dto.contentMatchType() : IBMMQStub.ContentMatchType.NONE);
        entity.setContentPattern(dto.contentPattern());
        entity.setCaseSensitive(dto.caseSensitive() != null ? dto.caseSensitive() : false);
        entity.setPriority(dto.priority() != null ? dto.priority() : 0);
        
        entity.setResponseContent(dto.responseContent());
        entity.setResponseType(dto.responseType());
        entity.setLatency(dto.latency());
        entity.setHeaders(dto.headers());
        entity.setStatus(dto.status() != null ? dto.status() : StubStatus.INACTIVE);

        // Handle dates - use DTO dates if present, otherwise use current time
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : now);
        entity.setUpdatedAt(dto.updatedAt() != null ? dto.updatedAt() : now);

        return entity;
    }

    /**
     * Convert an entity to a DTO
     */
    private IBMMQStubDTO convertToDTO(IBMMQStub entity) {
        return new IBMMQStubDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getUserId(),
                entity.getQueueManager(),
                entity.getQueueName(),
                entity.getSelector(),
                
                // Standardized content matching fields
                entity.getContentMatchType(),
                entity.getContentPattern(),
                entity.isCaseSensitive(),
                entity.getPriority(),
                
                entity.getResponseContent(),
                entity.getResponseType(),
                entity.getLatency(),
                entity.getHeaders(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
} 
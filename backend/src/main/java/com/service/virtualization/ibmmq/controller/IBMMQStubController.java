package com.service.virtualization.ibmmq.controller;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.service.IBMMQStubService;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"}, allowCredentials = "true")
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
    public ResponseEntity<IBMMQStub> createStub(@RequestBody IBMMQStub stub) {
        logger.info("Creating new IBM MQ stub: {}", stub.getName());

        // Save the entity
        IBMMQStub createdStub = ibmMQStubService.create(stub);

        // Convert back to DTO and return
        return new ResponseEntity<>(createdStub, HttpStatus.CREATED);
    }

    /**
     * Get all IBM MQ stubs
     */
    @GetMapping
    public ResponseEntity<List<IBMMQStub>> getAllStubs() {
        logger.info("Fetching all IBM MQ stubs");

        List<IBMMQStub> stubs = ibmMQStubService.findAll();

        return ResponseEntity.ok(stubs);
    }

    /**
     * Get IBM MQ stubs by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<IBMMQStub>> getStubsByUser(@PathVariable String userId) {
        logger.info("Fetching IBM MQ stubs for user: {}", userId);

        List<IBMMQStub> stubs = ibmMQStubService.findByUserId(userId);

        return ResponseEntity.ok(stubs);
    }

    /**
     * Get a IBM MQ stub by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IBMMQStub> getStubById(@PathVariable String id) {
        logger.info("Fetching IBM MQ stub with ID: {}", id);

        Optional<IBMMQStub> stubOptional = ibmMQStubService.findById(id);

        return stubOptional
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing IBM MQ stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<IBMMQStub> updateStub(@PathVariable String id, @RequestBody IBMMQStub stub) {
        logger.info("Updating IBM MQ stub with ID: {}", id);


        // Update the entity
        IBMMQStub updatedStub = ibmMQStubService.update(id, stub);

        // Convert back to DTO and return
        return ResponseEntity.ok(updatedStub);
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
    public ResponseEntity<IBMMQStub> updateStubStatus(@PathVariable String id, @RequestBody Map<String, String> statusUpdate) {
        StubStatus status = StubStatus.valueOf(statusUpdate.get("status"));

        if (status == null) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Updating status of IBM MQ stub with ID: {} to {}", id, status);

        IBMMQStub updatedStub = ibmMQStubService.updateStatus(id, status);

        return ResponseEntity.ok(updatedStub);
    }

    /**
     * Toggle the status of an IBM MQ stub
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<IBMMQStub> toggleStubStatus(@PathVariable String id) {
        logger.info("Toggling status of IBM MQ stub with ID: {}", id);

        IBMMQStub updatedStub = ibmMQStubService.toggleStubStatus(id);

        return ResponseEntity.ok(updatedStub);
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
}
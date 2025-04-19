package com.service.virtualization.activemq.controller;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.service.ActiveMQStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing ActiveMQ stubs.
 */
@RestController
@RequestMapping("/api/activemq/stubs")
public class ActiveMQStubController {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQStubController.class);
    
    @Autowired
    private ActiveMQStubService activeMQStubService;
    
    /**
     * Get all ActiveMQ stubs.
     *
     * @return List of all stubs
     */
    @GetMapping
    public ResponseEntity<List<ActiveMQStub>> getAllStubs() {
        logger.info("Fetching all ActiveMQ stubs");
        return ResponseEntity.ok(activeMQStubService.getAllStubs());
    }
    
    /**
     * Get a specific ActiveMQ stub by ID.
     *
     * @param id The stub ID
     * @return The stub with the specified ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActiveMQStub> getStubById(@PathVariable String id) {
        logger.info("Fetching ActiveMQ stub with ID: {}", id);
        return ResponseEntity.ok(activeMQStubService.getStubById(id));
    }
    
    /**
     * Create a new ActiveMQ stub.
     *
     * @param stub The stub to create
     * @return The created stub
     */
    @PostMapping
    public ResponseEntity<?> createStub(@RequestBody ActiveMQStub stub) {
        logger.info("Creating a new ActiveMQ stub");
        try {
            return new ResponseEntity<>(activeMQStubService.createStub(stub), HttpStatus.CREATED);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("higher priority")) {
                // Priority conflict
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "priority_conflict");
                errorResponse.put("message", e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
            }
            throw e; // Re-throw other exceptions
        }
    }
    
    /**
     * Update an existing ActiveMQ stub.
     *
     * @param id The stub ID
     * @param stub The updated stub details
     * @return The updated stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<ActiveMQStub> updateStub(@PathVariable String id, @RequestBody ActiveMQStub stub) {
        logger.info("Updating ActiveMQ stub with ID: {}", id);
        return ResponseEntity.ok(activeMQStubService.updateStub(id, stub));
    }
    
    /**
     * Delete an ActiveMQ stub.
     *
     * @param id The stub ID
     * @return No content on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStub(@PathVariable String id) {
        logger.info("Deleting ActiveMQ stub with ID: {}", id);
        activeMQStubService.deleteStub(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Toggle the status of an ActiveMQ stub.
     *
     * @param id The stub ID
     * @return The updated stub
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ActiveMQStub> toggleStubStatus(@PathVariable String id) {
        logger.info("Toggling status of ActiveMQ stub with ID: {}", id);
        return ResponseEntity.ok(activeMQStubService.toggleStubStatus(id));
    }
    
    /**
     * Get ActiveMQ stubs for a specific user.
     *
     * @param userId The user ID
     * @return List of stubs belonging to the specified user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActiveMQStub>> getStubsByUser(@PathVariable String userId) {
        logger.info("Fetching ActiveMQ stubs for user ID: {}", userId);
        return ResponseEntity.ok(activeMQStubService.getStubsByUser(userId));
    }
} 
package com.service.virtualization.controller;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.ActiveMQStub;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.service.ActiveMQStubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for ActiveMQStub operations
 */
@RestController
@RequestMapping("/api/activemq/stubs")
@CrossOrigin(origins = "*")
public class ActiveMQStubController {

    private final ActiveMQStubService activeMQStubService;
    
    @Autowired
    public ActiveMQStubController(ActiveMQStubService activeMQStubService) {
        this.activeMQStubService = activeMQStubService;
    }
    
    /**
     * Get all ActiveMQ stubs
     * @return list of all ActiveMQ stubs
     */
    @GetMapping
    public ResponseEntity<List<ActiveMQStub>> getAllStubs() {
        return ResponseEntity.ok(activeMQStubService.findAll());
    }
    
    /**
     * Get ActiveMQ stubs by user ID
     * @param userId the user ID
     * @return list of ActiveMQ stubs
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActiveMQStub>> getStubsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(activeMQStubService.findByUserId(userId));
    }
    
    /**
     * Get active ActiveMQ stubs by user ID
     * @param userId the user ID
     * @return list of active ActiveMQ stubs
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<ActiveMQStub>> getActiveStubsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(activeMQStubService.findActiveByUserId(userId));
    }
    
    /**
     * Get an ActiveMQ stub by ID
     * @param id the ActiveMQ stub ID
     * @return the ActiveMQ stub
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActiveMQStub> getStubById(@PathVariable String id) {
        return activeMQStubService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with ID: " + id));
    }
    
    /**
     * Create a new ActiveMQ stub
     * @param activeMQStub the ActiveMQ stub to create
     * @return the created ActiveMQ stub
     */
    @PostMapping
    public ResponseEntity<ActiveMQStub> createStub(@RequestBody ActiveMQStub activeMQStub) {
        return new ResponseEntity<>(activeMQStubService.create(activeMQStub), HttpStatus.CREATED);
    }
    
    /**
     * Update an existing ActiveMQ stub
     * @param id the ActiveMQ stub ID
     * @param activeMQStub the updated ActiveMQ stub
     * @return the updated ActiveMQ stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<ActiveMQStub> updateStub(@PathVariable String id, @RequestBody ActiveMQStub activeMQStub) {
        return ResponseEntity.ok(activeMQStubService.update(id, activeMQStub));
    }
    
    /**
     * Delete an ActiveMQ stub
     * @param id the ActiveMQ stub ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStub(@PathVariable String id) {
        activeMQStubService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update an ActiveMQ stub's status
     * @param id the ActiveMQ stub ID
     * @param statusMap map with status field
     * @return the updated ActiveMQ stub
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ActiveMQStub> updateStubStatus(
            @PathVariable String id, 
            @RequestBody Map<String, Boolean> statusMap) {
        
        Boolean status = statusMap.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(activeMQStubService.updateStatus(id, status));
    }
    
    /**
     * Add a header to an ActiveMQ stub
     * @param id the ActiveMQ stub ID
     * @param header the header to add
     * @return the updated ActiveMQ stub
     */
    @PostMapping("/{id}/headers")
    public ResponseEntity<ActiveMQStub> addHeader(
            @PathVariable String id,
            @RequestBody MessageHeader header) {
        
        return ResponseEntity.ok(activeMQStubService.addHeader(id, header));
    }
    
    /**
     * Remove a header from an ActiveMQ stub
     * @param id the ActiveMQ stub ID
     * @param headerName the name of the header to remove
     * @return the updated ActiveMQ stub
     */
    @DeleteMapping("/{id}/headers/{headerName}")
    public ResponseEntity<ActiveMQStub> removeHeader(
            @PathVariable String id,
            @PathVariable String headerName) {
        
        return ResponseEntity.ok(activeMQStubService.removeHeader(id, headerName));
    }
    
    /**
     * Find ActiveMQ stubs by destination name
     * @param destinationName the destination name
     * @return list of ActiveMQ stubs
     */
    @GetMapping("/destination")
    public ResponseEntity<List<ActiveMQStub>> getStubsByDestinationName(
            @RequestParam String destinationName) {
        
        return ResponseEntity.ok(activeMQStubService.findByDestinationName(destinationName));
    }
    
    /**
     * Find ActiveMQ stubs by destination type and name
     * @param destinationType the destination type
     * @param destinationName the destination name
     * @return list of ActiveMQ stubs
     */
    @GetMapping("/destination/type")
    public ResponseEntity<List<ActiveMQStub>> getStubsByDestinationTypeAndName(
            @RequestParam String destinationType,
            @RequestParam String destinationName) {
        
        return ResponseEntity.ok(activeMQStubService.findByDestinationTypeAndName(destinationType, destinationName));
    }
    
    /**
     * Find active ActiveMQ stubs by destination type and name
     * @param destinationType the destination type
     * @param destinationName the destination name
     * @return list of active ActiveMQ stubs
     */
    @GetMapping("/destination/active")
    public ResponseEntity<List<ActiveMQStub>> getActiveStubsByDestinationTypeAndName(
            @RequestParam String destinationType,
            @RequestParam String destinationName) {
        
        return ResponseEntity.ok(activeMQStubService.findActiveByDestinationTypeAndName(destinationType, destinationName));
    }
    
    /**
     * Publish a message to an ActiveMQ destination
     * @param messageRequest the message request containing destination and content
     * @return response with success status
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishMessage(@RequestBody Map<String, Object> messageRequest) {
        try {
            String destinationType = (String) messageRequest.get("destinationType");
            String destinationName = (String) messageRequest.get("destinationName");
            String message = (String) messageRequest.get("message");
            
            @SuppressWarnings("unchecked")
            List<Map<String, String>> headerMaps = (List<Map<String, String>>) messageRequest.getOrDefault("headers", List.of());
            
            // Convert header maps to MessageHeader objects
            List<MessageHeader> headers = headerMaps.stream()
                    .map(map -> new MessageHeader(
                            map.get("name"),
                            map.get("value"),
                            map.getOrDefault("type", "string")))
                    .toList();
            
            boolean success = activeMQStubService.publishMessage(destinationType, destinationName, message, headers);
            
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Invalid request: " + e.getMessage()
            ));
        }
    }
} 
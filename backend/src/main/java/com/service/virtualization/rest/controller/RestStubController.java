package com.service.virtualization.rest.controller;

import com.service.virtualization.dto.DtoConverter;
import com.service.virtualization.model.RestStub;
import com.service.virtualization.model.Stub;
import com.service.virtualization.dto.RestStubDTO;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.rest.service.impl.RestStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static com.service.virtualization.dto.DtoConverter.fromRestStub;
import static com.service.virtualization.dto.DtoConverter.toRestStub;

/**
 * REST controller for stub operations
 */
@RestController
@RequestMapping("/rest/stubs")
public class RestStubController {
    
    private static final Logger logger = LoggerFactory.getLogger(RestStubController.class);
    
    private final RestStubService restStubService;
    
    public RestStubController(RestStubService restStubService) {
        this.restStubService = restStubService;
    }
    
    /**
     * Get all stubs
     */
    @GetMapping
    public ResponseEntity<List<RestStubDTO>> getAllStubs() {
        logger.debug("Getting all stubs");
        List<RestStubDTO> restStubDTOS = restStubService.findAllStubs().stream()
                .map(DtoConverter::fromRestStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(restStubDTOS);
    }
    
    /**
     * Get stub by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestStubDTO> getStubById(@PathVariable String id) {
        logger.debug("Getting stub with ID: {}", id);
        return restStubService.findStubById(id)
                .map(DtoConverter::fromRestStub)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stub not found with ID: " + id));
    }
    
    /**
     * Get stubs by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RestStubDTO>> getStubsByUserId(@PathVariable String userId) {
        logger.debug("Getting stubs for user: {}", userId);
        List<RestStubDTO> restStubDTOS = restStubService.findStubsByUserId(userId).stream()
                .map(DtoConverter::fromRestStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(restStubDTOS);
    }
    
    /**
     * Create a new stub
     */
    @PostMapping
    public ResponseEntity<RestStubDTO> createStub(@RequestBody RestStubDTO restStubDTO) {
        logger.debug("Creating rest stub: {}", restStubDTO.name());
        
        // Convert DTO to domain model
        RestStub restStub = toRestStub(restStubDTO);
        
        // Create the stub
        RestStub createdStub = restStubService.createStub(restStub);
        
        // Convert back to DTO and return
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fromRestStub(createdStub));
    }
    
    /**
     * Update an existing stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<RestStubDTO> updateStub(@PathVariable String id, @RequestBody RestStubDTO restStubDTO) {
        logger.debug("Updating stub with ID: {}", id);
        
        // Ensure ID matches
        if (!id.equals(restStubDTO.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path must match ID in body");
        }
        
        // Convert DTO to domain model
        RestStub stub = toRestStub(restStubDTO);
        
        // Update the stub
        RestStub updatedStub = restStubService.updateStub(stub);
        
        // Convert back to DTO and return
        return ResponseEntity.ok(fromRestStub(updatedStub));
    }
    
    /**
     * Delete a stub
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStub(@PathVariable String id) {
        logger.debug("Deleting stub with ID: {}", id);
        restStubService.deleteStub(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update stub status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<RestStubDTO> updateStubStatus(@PathVariable String id, @RequestParam StubStatus status) {
        logger.debug("Updating status of stub {} to {}", id, status);
        
        return restStubService.findStubById(id)
                .map(stub -> {
                    RestStub updatedStub = stub.withStatus(status);
                    updatedStub = restStubService.updateStub(updatedStub);
                    return ResponseEntity.ok(fromRestStub(updatedStub));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stub not found with ID: " + id));
    }
//
//    /**
//     * Convert a recording to a stub
//     */
//    @PostMapping("/convert-recording/{recordingId}")
//    public ResponseEntity<RestStubDTO> convertRecordingToStub(@PathVariable String recordingId) {
//        logger.debug("Converting recording to stub: {}", recordingId);
//        Stub stub = restStubService.convertRecordingToStub(recordingId);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(RestStubDTO.fromStub(stub));
//    }
} 
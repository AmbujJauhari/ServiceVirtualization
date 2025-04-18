package com.service.virtualization.soap.controller;

import com.service.virtualization.dto.DtoConverter;
import com.service.virtualization.dto.SoapStubDTO;
import com.service.virtualization.model.SoapStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.soap.service.SoapStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for SOAP stub operations
 */
@RestController
@RequestMapping("/soap/stubs")
public class SoapStubController {
    
    private static final Logger logger = LoggerFactory.getLogger(SoapStubController.class);
    
    private final SoapStubService soapStubService;
    
    public SoapStubController(SoapStubService soapStubService) {
        this.soapStubService = soapStubService;
    }
    
    /**
     * Get all SOAP stubs
     */
    @GetMapping
    public ResponseEntity<List<SoapStubDTO>> getAllStubs() {
        logger.debug("Getting all SOAP stubs");
        List<SoapStubDTO> soapStubDTOs = soapStubService.findAllStubs().stream()
                .map(DtoConverter::fromSoapStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(soapStubDTOs);
    }
    
    /**
     * Get SOAP stub by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SoapStubDTO> getStubById(@PathVariable String id) {
        logger.debug("Getting SOAP stub with ID: {}", id);
        return soapStubService.findStubById(id)
                .map(DtoConverter::fromSoapStub)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOAP stub not found with ID: " + id));
    }
    
    /**
     * Get SOAP stubs by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SoapStubDTO>> getStubsByUserId(@PathVariable String userId) {
        logger.debug("Getting SOAP stubs for user: {}", userId);
        List<SoapStubDTO> soapStubDTOs = soapStubService.findStubsByUserId(userId).stream()
                .map(DtoConverter::fromSoapStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(soapStubDTOs);
    }
    
    /**
     * Get SOAP stubs by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SoapStubDTO>> getStubsByStatus(@PathVariable String status) {
        logger.debug("Getting SOAP stubs with status: {}", status);
        try {
            StubStatus stubStatus = StubStatus.valueOf(status.toUpperCase());
            List<SoapStubDTO> soapStubDTOs = soapStubService.findStubsByStatus(stubStatus).stream()
                    .map(DtoConverter::fromSoapStub)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(soapStubDTOs);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
    }
    
    /**
     * Get SOAP stubs by service name
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<SoapStubDTO>> getStubsByServiceName(@PathVariable String serviceName) {
        logger.debug("Getting SOAP stubs for service: {}", serviceName);
        List<SoapStubDTO> soapStubDTOs = soapStubService.findStubsByServiceName(serviceName).stream()
                .map(DtoConverter::fromSoapStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(soapStubDTOs);
    }
    
    /**
     * Get SOAP stubs by operation name
     */
    @GetMapping("/operation/{operationName}")
    public ResponseEntity<List<SoapStubDTO>> getStubsByOperationName(@PathVariable String operationName) {
        logger.debug("Getting SOAP stubs for operation: {}", operationName);
        List<SoapStubDTO> soapStubDTOs = soapStubService.findStubsByOperationName(operationName).stream()
                .map(DtoConverter::fromSoapStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(soapStubDTOs);
    }
    
    /**
     * Create a new SOAP stub
     */
    @PostMapping
    public ResponseEntity<SoapStubDTO> createStub(@RequestBody SoapStubDTO soapStubDTO) {
        logger.debug("Creating SOAP stub: {}", soapStubDTO.name());
        
        // Convert DTO to domain model
        SoapStub soapStub = DtoConverter.toSoapStub(soapStubDTO);
        
        // Create the stub
        SoapStub createdStub = soapStubService.createStub(soapStub);
        
        // Convert back to DTO and return
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoConverter.fromSoapStub(createdStub));
    }
    
    /**
     * Update an existing SOAP stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<SoapStubDTO> updateStub(@PathVariable String id, @RequestBody SoapStubDTO soapStubDTO) {
        logger.debug("Updating SOAP stub with ID: {}", id);
        
        // Ensure ID matches
        if (!id.equals(soapStubDTO.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path must match ID in body");
        }
        
        // Convert DTO to domain model
        SoapStub soapStub = DtoConverter.toSoapStub(soapStubDTO);
        
        // Update the stub
        SoapStub updatedStub = soapStubService.updateStub(soapStub);
        
        // Convert back to DTO and return
        return ResponseEntity.ok(DtoConverter.fromSoapStub(updatedStub));
    }
    
    /**
     * Delete a SOAP stub
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStub(@PathVariable String id) {
        logger.debug("Deleting SOAP stub with ID: {}", id);
        soapStubService.deleteStub(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update SOAP stub status
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<SoapStubDTO> updateStubStatus(
            @PathVariable String id,
            @RequestParam(name = "active") boolean active) {
        
        logger.debug("Updating SOAP stub status to {}: {}", active ? "ACTIVE" : "INACTIVE", id);
        
        // Update the status
        StubStatus newStatus = active ? StubStatus.ACTIVE : StubStatus.INACTIVE;
        SoapStub updatedStub = soapStubService.updateStubStatus(id, newStatus);
        
        // Convert to DTO and return
        return ResponseEntity.ok(DtoConverter.fromSoapStub(updatedStub));
    }
} 
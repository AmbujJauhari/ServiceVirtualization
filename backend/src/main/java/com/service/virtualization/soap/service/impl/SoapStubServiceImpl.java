package com.service.virtualization.soap.service.impl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.service.virtualization.soap.SoapStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.soap.SoapStubRepository;
import com.service.virtualization.soap.service.SoapStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the SoapStubService interface
 */
@Service
public class SoapStubServiceImpl implements SoapStubService {

    private static final Logger logger = LoggerFactory.getLogger(SoapStubServiceImpl.class);
    
    private final SoapStubRepository soapStubRepository;
    private final WireMockServer wireMockServer;
    
    public SoapStubServiceImpl(SoapStubRepository soapStubRepository, WireMockServer wireMockServer) {
        this.soapStubRepository = soapStubRepository;
        this.wireMockServer = wireMockServer;
    }
    
    @Override
    public SoapStub createStub(SoapStub stub) {
        // Validate the stub
        if (!stub.isValid()) {
            throw new IllegalArgumentException("Invalid SOAP stub data");
        }
        
        // Generate ID if not present
        String stubId = stub.id() != null ? stub.id() : UUID.randomUUID().toString();
        
        // Create a stub with updated timestamps
        SoapStub newStub = new SoapStub(
            stubId,
            stub.name(),
            stub.description(),
            stub.userId(),
            stub.behindProxy(),
            stub.protocol(),
            stub.tags(),
            stub.status(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null, // WireMock mapping ID will be set after registration
            stub.wsdlUrl(),
            stub.serviceName(),
            stub.portName(),
            stub.operationName(),
            stub.matchConditions(),
            stub.response()
        );
        
        // Register with WireMock if stub is active
        if (stub.status() == StubStatus.ACTIVE) {
            String mappingId = registerWithWireMock(newStub);
            newStub = updateWireMockMappingId(newStub, mappingId);
        }
        
        // Save to database
        return soapStubRepository.save(newStub);
    }
    
    @Override
    public SoapStub updateStub(SoapStub stub) {
        // Check if stub exists
        if (!soapStubRepository.existsById(stub.id())) {
            throw new IllegalArgumentException("SOAP stub not found with ID: " + stub.id());
        }
        
        // Get existing stub to preserve creation date
        Optional<SoapStub> existingStubOpt = soapStubRepository.findById(stub.id());
        if (existingStubOpt.isEmpty()) {
            throw new IllegalArgumentException("SOAP stub not found with ID: " + stub.id());
        }
        
        SoapStub existingStub = existingStubOpt.get();
        
        // Create updated stub
        SoapStub updatedStub = new SoapStub(
            stub.id(),
            stub.name(),
            stub.description(),
            stub.userId(),
            stub.behindProxy(),
            stub.protocol(),
            stub.tags(),
            stub.status(),
            existingStub.createdAt(),
            LocalDateTime.now(),
            existingStub.wiremockMappingId(),
            stub.wsdlUrl(),
            stub.serviceName(),
            stub.portName(),
            stub.operationName(),
            stub.matchConditions(),
            stub.response()
        );
        
        // Update WireMock if stub is active
        if (updatedStub.status() == StubStatus.ACTIVE) {
            // Remove existing mapping if present
            if (existingStub.wiremockMappingId() != null) {
                removeWireMockStub(existingStub.wiremockMappingId());
            }
            
            // Register with WireMock
            String mappingId = registerWithWireMock(updatedStub);
            updatedStub = updateWireMockMappingId(updatedStub, mappingId);
        } else if (existingStub.wiremockMappingId() != null) {
            // Remove from WireMock if inactive
            removeWireMockStub(existingStub.wiremockMappingId());
            updatedStub = updateWireMockMappingId(updatedStub, null);
        }
        
        // Save to database
        return soapStubRepository.save(updatedStub);
    }
    
    @Override
    public void deleteStub(String id) {
        // Get the stub to check for WireMock mapping
        Optional<SoapStub> stubOpt = soapStubRepository.findById(id);
        if (stubOpt.isPresent()) {
            SoapStub stub = stubOpt.get();
            
            // Remove from WireMock if active
            if (stub.wiremockMappingId() != null) {
                removeWireMockStub(stub.wiremockMappingId());
            }
            
            // Delete from database
            soapStubRepository.deleteById(id);
            logger.info("Deleted SOAP stub with ID: {}", id);
        } else {
            logger.warn("Failed to delete SOAP stub: not found with ID: {}", id);
        }
    }
    
    @Override
    public Optional<SoapStub> findStubById(String id) {
        return soapStubRepository.findById(id);
    }
    
    @Override
    public List<SoapStub> findAllStubs() {
        return soapStubRepository.findAll();
    }
    
    @Override
    public List<SoapStub> findStubsByUserId(String userId) {
        return soapStubRepository.findByUserId(userId);
    }
    
    @Override
    public List<SoapStub> findStubsByStatus(StubStatus status) {
        return soapStubRepository.findByStatus(status);
    }
    
    @Override
    public List<SoapStub> findStubsByServiceName(String serviceName) {
        return soapStubRepository.findByServiceName(serviceName);
    }
    
    @Override
    public List<SoapStub> findStubsByOperationName(String operationName) {
        return soapStubRepository.findByOperationName(operationName);
    }
    
    @Override
    public SoapStub updateStubStatus(String id, StubStatus status) {
        // Find the stub
        Optional<SoapStub> stubOpt = soapStubRepository.findById(id);
        if (stubOpt.isEmpty()) {
            throw new IllegalArgumentException("SOAP stub not found with ID: " + id);
        }
        
        SoapStub stub = stubOpt.get();
        
        // Skip if status is unchanged
        if (stub.status() == status) {
            return stub;
        }
        
        // Create updated stub with new status
        SoapStub updatedStub = new SoapStub(
            stub.id(),
            stub.name(),
            stub.description(),
            stub.userId(),
            stub.behindProxy(),
            stub.protocol(),
            stub.tags(),
            status,
            stub.createdAt(),
            LocalDateTime.now(),
            stub.wiremockMappingId(),
            stub.wsdlUrl(),
            stub.serviceName(),
            stub.portName(),
            stub.operationName(),
            stub.matchConditions(),
            stub.response()
        );
        
        // Update WireMock registration
        if (status == StubStatus.ACTIVE) {
            // Register with WireMock if active
            String mappingId = registerWithWireMock(updatedStub);
            updatedStub = updateWireMockMappingId(updatedStub, mappingId);
        } else if (stub.wiremockMappingId() != null) {
            // Remove from WireMock if inactive
            removeWireMockStub(stub.wiremockMappingId());
            updatedStub = updateWireMockMappingId(updatedStub, null);
        }
        
        // Save updated stub
        return soapStubRepository.save(updatedStub);
    }
    
    // Helper methods
    
    /**
     * Register a SOAP stub with WireMock
     */
    private String registerWithWireMock(SoapStub stub) {
        try {
            // Extract required data from stub
            Map<String, Object> matchConditions = stub.matchConditions();
            Map<String, Object> response = stub.response();
            
            // Build the WireMock mapping
            StubMapping mapping = buildWireMockMapping(stub, matchConditions, response);
            
            // Register with WireMock
            wireMockServer.addStubMapping(mapping);
            logger.info("Registered SOAP stub with WireMock: {}", stub.name());
            
            return mapping.getId().toString();
        } catch (Exception e) {
            logger.error("Failed to register SOAP stub with WireMock: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Remove a WireMock stub by ID
     */
    private void removeWireMockStub(String mappingId) {
        try {
            if (mappingId != null) {
                UUID id = UUID.fromString(mappingId);
                wireMockServer.removeStubMapping(StubMapping.buildFrom("{\"id\": \"" + id + "\"}"));
                logger.info("Removed WireMock stub with ID: {}", mappingId);
            }
        } catch (Exception e) {
            logger.error("Failed to remove WireMock stub: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Build a WireMock mapping for SOAP request/response
     */
    private StubMapping buildWireMockMapping(SoapStub stub, Map<String, Object> matchConditions, Map<String, Object> responseData) {
        // Get service path for SOAP endpoint
        String servicePath = getSoapEndpointPath(stub);
        
        // Create SOAP request matcher
        MappingBuilder requestBuilder = WireMock.post(WireMock.urlPathEqualTo(servicePath))
            .withHeader("Content-Type", WireMock.containing("text/xml"))
            .withHeader("SOAPAction", WireMock.containing(stub.operationName()));
        
        // Add XML body matcher if provided
        if (matchConditions.containsKey("body")) {
            String requestBody = (String) matchConditions.get("body");
            requestBuilder = requestBuilder.withRequestBody(WireMock.containing(requestBody));
        }
        
        // Create the response
        ResponseDefinitionBuilder responseBuilder = WireMock.aResponse()
            .withStatus(getResponseStatus(responseData))
            .withHeader("Content-Type", "text/xml; charset=utf-8");
        
        // Add body if present
        if (responseData.containsKey("body")) {
            responseBuilder = responseBuilder.withBody((String) responseData.get("body"));
        }
        
        // Build and return the stub mapping
        return requestBuilder.willReturn(responseBuilder).build();
    }
    
    /**
     * Create a new stub with updated WireMock mapping ID
     */
    private SoapStub updateWireMockMappingId(SoapStub stub, String mappingId) {
        return new SoapStub(
            stub.id(),
            stub.name(),
            stub.description(),
            stub.userId(),
            stub.behindProxy(),
            stub.protocol(),
            stub.tags(),
            stub.status(),
            stub.createdAt(),
            stub.updatedAt(),
            mappingId,
            stub.wsdlUrl(),
            stub.serviceName(),
            stub.portName(),
            stub.operationName(),
            stub.matchConditions(),
            stub.response()
        );
    }
    
    /**
     * Get response status from response data
     */
    private int getResponseStatus(Map<String, Object> responseData) {
        if (responseData.containsKey("status")) {
            Object status = responseData.get("status");
            if (status instanceof Integer) {
                return (Integer) status;
            } else if (status instanceof String) {
                return Integer.parseInt((String) status);
            }
        }
        return 200; // Default status for SOAP is 200 OK
    }
    
    /**
     * Get the SOAP endpoint path from stub
     */
    private String getSoapEndpointPath(SoapStub stub) {
        return "/soap/" + stub.serviceName(); // Default convention
    }
} 
package com.service.virtualization.soap;

import com.service.virtualization.model.Protocol;
import com.service.virtualization.model.StubStatus;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Model for SOAP service stub with SOAP-specific attributes
 */
public record SoapStub(
        String id,
        String name,
        String description,
        String userId,
        boolean behindProxy,
        String protocol,
        List<String> tags,
        StubStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String wiremockMappingId,
        
        // SOAP specific fields
        String wsdlUrl,
        String serviceName,
        String portName,
        String operationName,
        
        Map<String, Object> matchConditions,
        Map<String, Object> response
) {
    public SoapStub() {
        this(
                UUID.randomUUID().toString(),
                null,
                null,
                null,
                false,
                Protocol.SOAP.name(),
                new ArrayList<>(),
                StubStatus.INACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                new HashMap<>(),
                new HashMap<>()
        );
    }

    /**
     * Canonical constructor with validation
     */
    public SoapStub {
        // Ensure defaults for null values
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (protocol == null) {
            protocol = Protocol.SOAP.name();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = StubStatus.INACTIVE;
        }
        if (matchConditions == null) {
            matchConditions = new HashMap<>();
        }
        if (response == null) {
            response = new HashMap<>();
        }
    }
    
    /**
     * Create a new Stub with the updated matching conditions
     */
    public SoapStub withMatchingConditions(Map<String, Object> matchConditions) {
        return new SoapStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), wiremockMappingId, wsdlUrl, serviceName, 
                portName, operationName, matchConditions, response
        );
    }

    /**
     * Create a new Stub with the updated response data
     */
    public SoapStub withResponse(Map<String, Object> newResponse) {
        return new SoapStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), wiremockMappingId, wsdlUrl, serviceName,
                portName, operationName, matchConditions, newResponse
        );
    }

    /**
     * Create a new Stub with the updated status
     */
    public SoapStub withStatus(StubStatus newStatus) {
        return new SoapStub(
                id, name, description, userId, behindProxy, protocol, tags, newStatus,
                createdAt, LocalDateTime.now(), wiremockMappingId, wsdlUrl, serviceName,
                portName, operationName, matchConditions, response
        );
    }

    /**
     * Create a new Stub with updated WSDL information
     */
    public SoapStub withWsdlInfo(String newWsdlUrl, String newServiceName, String newPortName, String newOperationName) {
        return new SoapStub(
                id, name, description, userId, behindProxy, protocol, tags, status,
                createdAt, LocalDateTime.now(), wiremockMappingId, newWsdlUrl, newServiceName,
                newPortName, newOperationName, matchConditions, response
        );
    }

    /**
     * Validates if the stub has all required fields populated
     * @return true if the stub is valid, false otherwise
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
                && protocol != null
                && !matchConditions.isEmpty()
                && !response.isEmpty();
    }
} 
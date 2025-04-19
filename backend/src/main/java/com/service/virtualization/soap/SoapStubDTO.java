package com.service.virtualization.soap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for SOAP stub operations
 */
public record SoapStubDTO(
        String id,
        String name,
        String description,
        String userId,
        boolean behindProxy,
        String protocol,
        List<String> tags,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        
        // SOAP specific fields
        String wsdlUrl,
        String serviceName,
        String portName,
        String operationName,
        
        Map<String, Object> matchConditions,
        Map<String, Object> response
) {
} 
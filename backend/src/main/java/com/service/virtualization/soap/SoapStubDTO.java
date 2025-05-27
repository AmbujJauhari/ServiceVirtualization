package com.service.virtualization.soap;

import java.util.List;
import java.util.Map;

/**
 * DTO for SOAP service stub with simplified fields
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
        String createdAt,
        String updatedAt,
        
        // SOAP specific fields (simplified)
        String url,
        String soapAction,
        String webhookUrl,
        
        Map<String, Object> matchConditions,
        Map<String, Object> response
) {
} 
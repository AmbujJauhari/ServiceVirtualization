package com.service.virtualization.dto;

import com.service.virtualization.model.RestStub;
import com.service.virtualization.model.SoapStub;
import com.service.virtualization.model.StubStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class for converting between DTOs and domain models
 */
public class DtoConverter {

    /**
     * Convert from RestStub domain model to RestStubDTO
     */
    public static RestStubDTO fromRestStub(RestStub restStub) {
        if (restStub == null) {
            return null;
        }
        
        return new RestStubDTO(
                restStub.id(),
                restStub.name(),
                restStub.description(),
                restStub.userId(),
                restStub.behindProxy(),
                restStub.protocol(),
                restStub.tags(),
                restStub.status().name(),
                restStub.createdAt(),
                restStub.updatedAt(),
                restStub.wiremockMappingId(),
                restStub.matchConditions(),
                restStub.response()
        );
    }

    /**
     * Convert from RestStubDTO to RestStub domain model
     */
    public static RestStub toRestStub(RestStubDTO dto) {
        if (dto == null) {
            return null;
        }
        
        StubStatus status = (dto.status() != null) 
                ? StubStatus.valueOf(dto.status()) 
                : StubStatus.INACTIVE;
                
        return new RestStub(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.userId(),
                dto.behindProxy(),
                dto.protocol(),
                dto.tags() != null ? dto.tags() : new ArrayList<>(),
                status,
                dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now(),
                dto.updatedAt() != null ? dto.updatedAt() : LocalDateTime.now(),
                dto.wiremockMappingId(),
                dto.matchConditions() != null ? dto.matchConditions() : new HashMap<>(),
                dto.response() != null ? dto.response() : new HashMap<>()
        );
    }
    
    /**
     * Convert from SoapStub domain model to SoapStubDTO
     */
    public static SoapStubDTO fromSoapStub(SoapStub soapStub) {
        if (soapStub == null) {
            return null;
        }
        
        return new SoapStubDTO(
                soapStub.id(),
                soapStub.name(),
                soapStub.description(),
                soapStub.userId(),
                soapStub.behindProxy(),
                soapStub.protocol(),
                soapStub.tags(),
                soapStub.status().name(),
                soapStub.createdAt(),
                soapStub.updatedAt(),
                soapStub.wsdlUrl(),
                soapStub.serviceName(),
                soapStub.portName(),
                soapStub.operationName(),
                soapStub.matchConditions(),
                soapStub.response()
        );
    }

    /**
     * Convert from SoapStubDTO to SoapStub domain model
     */
    public static SoapStub toSoapStub(SoapStubDTO dto) {
        if (dto == null) {
            return null;
        }
        
        StubStatus status = (dto.status() != null) 
                ? StubStatus.valueOf(dto.status()) 
                : StubStatus.INACTIVE;
                
        return new SoapStub(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.userId(),
                dto.behindProxy(),
                dto.protocol(),
                dto.tags() != null ? dto.tags() : new ArrayList<>(),
                status,
                dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now(),
                dto.updatedAt() != null ? dto.updatedAt() : LocalDateTime.now(),
                null, // wiremockMappingId
                dto.wsdlUrl(),
                dto.serviceName(),
                dto.portName(),
                dto.operationName(),
                dto.matchConditions() != null ? dto.matchConditions() : new HashMap<>(),
                dto.response() != null ? dto.response() : new HashMap<>()
        );
    }
}

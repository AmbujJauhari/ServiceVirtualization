package com.service.virtualization.kafka.mapper;

import com.service.virtualization.kafka.dto.KafkaStubDTO;
import com.service.virtualization.kafka.model.KafkaStub;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between KafkaStub entity and DTO
 */
@Component
public class KafkaStubMapper {

    /**
     * Convert entity to DTO
     *
     * @param stub The KafkaStub entity
     * @return The KafkaStubDTO
     */
    public KafkaStubDTO toDTO(KafkaStub stub) {
        return new KafkaStubDTO(
            stub.getId(),
            stub.getName(),
            stub.getDescription(),
            stub.getUserId(),
            stub.getRequestTopic(),
            stub.getResponseTopic(),
            
            // Content formats and matching
            stub.getRequestContentFormat(),
            stub.getResponseContentFormat(),
            stub.getRequestContentMatcher(),
            
            // Key matching
            stub.getKeyMatchType(),
            stub.getKeyPattern(),
            
            // Content/Value matching
            stub.getContentMatchType(),
            stub.getValuePattern(), // Legacy field
            stub.getContentPattern(), // New field
            stub.getCaseSensitive(),
            
            // Response configuration
            stub.getResponseType(),
            stub.getResponseKey(),
            stub.getResponseContent(),
            
            // Schema Registry fields
            stub.getUseResponseSchemaRegistry(),
            stub.getResponseSchemaId(),
            stub.getResponseSchemaSubject(),
            stub.getResponseSchemaVersion(),
            
            stub.getLatency(),
            
            // Callback configuration
            stub.getCallbackUrl(),
            stub.getCallbackHeaders(),
            
            stub.getStatus(),
            stub.getCreatedAt(),
            stub.getUpdatedAt()
        );
    }

    /**
     * Convert DTO to entity
     *
     * @param dto The KafkaStubDTO
     * @return The KafkaStub entity
     */
    public KafkaStub toModel(KafkaStubDTO dto) {
        KafkaStub stub = new KafkaStub();
        stub.setId(dto.id());
        stub.setName(dto.name());
        stub.setDescription(dto.description());
        stub.setUserId(dto.userId());
        stub.setRequestTopic(dto.requestTopic());
        stub.setResponseTopic(dto.responseTopic());
        
        // Content formats and matching
        stub.setRequestContentFormat(dto.requestContentFormat());
        stub.setResponseContentFormat(dto.responseContentFormat());
        stub.setRequestContentMatcher(dto.requestContentMatcher());
        
        // Key matching
        stub.setKeyMatchType(dto.keyMatchType());
        stub.setKeyPattern(dto.keyPattern());
        
        // Content/Value matching
        stub.setContentMatchType(dto.contentMatchType());
        stub.setValuePattern(dto.valuePattern()); // Legacy field
        stub.setContentPattern(dto.contentPattern()); // New field
        stub.setCaseSensitive(dto.caseSensitive());
        
        // Response configuration
        stub.setResponseType(dto.responseType());
        stub.setResponseKey(dto.responseKey());
        stub.setResponseContent(dto.responseContent());
        
        // Callback configuration
        stub.setCallbackUrl(dto.callbackUrl());
        stub.setCallbackHeaders(dto.callbackHeaders());
        
        // Schema Registry fields (transient/UI-only)
        stub.setUseResponseSchemaRegistry(dto.useResponseSchemaRegistry());
        stub.setResponseSchemaId(dto.responseSchemaId());
        stub.setResponseSchemaSubject(dto.responseSchemaSubject());
        stub.setResponseSchemaVersion(dto.responseSchemaVersion());
        
        stub.setLatency(dto.latency());
        stub.setStatus(dto.status());
        stub.setCreatedAt(dto.createdAt());
        stub.setUpdatedAt(dto.updatedAt());
        
        return stub;
    }
} 
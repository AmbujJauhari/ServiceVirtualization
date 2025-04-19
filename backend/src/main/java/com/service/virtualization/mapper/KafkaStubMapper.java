package com.service.virtualization.mapper;

import com.service.virtualization.dto.KafkaStubDTO;
import com.service.virtualization.model.KafkaStub;
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
            stub.getTopic(),
            stub.getKeyPattern(),
            stub.getValuePattern(),
            stub.getActiveForProducer(),
            stub.getActiveForConsumer(),
            stub.getResponseType(),
            stub.getResponseContent(),
            stub.getLatency(),
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
        stub.setTopic(dto.topic());
        stub.setKeyPattern(dto.keyPattern());
        stub.setValuePattern(dto.valuePattern());
        stub.setActiveForProducer(dto.activeForProducer());
        stub.setActiveForConsumer(dto.activeForConsumer());
        stub.setResponseType(dto.responseType());
        stub.setResponseContent(dto.responseContent());
        stub.setLatency(dto.latency());
        stub.setStatus(dto.status());
        stub.setCreatedAt(dto.createdAt());
        stub.setUpdatedAt(dto.updatedAt());
        
        return stub;
    }
} 
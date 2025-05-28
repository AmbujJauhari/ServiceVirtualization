package com.service.virtualization.kafka.mapper;

import com.service.virtualization.kafka.dto.KafkaStubDTO;
import com.service.virtualization.kafka.model.KafkaStub;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
                stub.id(),
                stub.name(),
                stub.description(),
                stub.userId(),
                stub.requestTopic(),
                stub.responseTopic(),

                // Content formats and matching
                stub.requestContentFormat(),
                stub.responseContentFormat(),
                stub.requestContentMatcher(),

                // Key matching
                stub.keyMatchType(),
                stub.keyPattern(),

                // Content/Value matching
                stub.contentMatchType(),
                stub.valuePattern(), // Legacy field
                stub.contentPattern(), // New field
                stub.caseSensitive(),

                // Response configuration
                stub.responseType(),
                stub.responseKey(),
                stub.responseContent(),

                // Schema Registry fields
                stub.useResponseSchemaRegistry(),
                stub.responseSchemaId(),
                stub.responseSchemaSubject(),
                stub.responseSchemaVersion(),

                stub.latency(),

                // Callback configuration
                stub.callbackUrl(),
                stub.callbackHeaders(),

                stub.status(),
                stub.createdAt(),
                stub.updatedAt(),
                stub.tags() != null ? stub.tags() : new ArrayList<>()
        );
    }

    /**
     * Convert DTO to entity
     *
     * @param dto The KafkaStubDTO
     * @return The KafkaStub entity
     */
    public KafkaStub toModel(KafkaStubDTO dto) {
        return new KafkaStub(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.userId(),
                dto.requestTopic(),
                dto.responseTopic(),

                // Content formats and matching
                dto.requestContentFormat(),
                dto.responseContentFormat(),
                dto.requestContentMatcher(),

                // Key matching
                dto.keyMatchType(),
                dto.keyPattern(),

                // Content/Value matching
                dto.contentMatchType(),
                dto.valuePattern(), // Legacy field
                dto.contentPattern(), // New field
                dto.caseSensitive(),

                // Response configuration
                dto.responseType(),
                dto.responseKey(),
                dto.responseContent(),

                // Schema Registry fields (transient/UI-only)
                dto.useResponseSchemaRegistry(),
                dto.responseSchemaId(),
                dto.responseSchemaSubject(),
                dto.responseSchemaVersion(),

                dto.latency(),
                dto.status(),
                dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now(),
                dto.updatedAt() != null ? dto.updatedAt() : LocalDateTime.now(),

                // Callback configuration
                dto.callbackUrl(),
                dto.callbackHeaders(),

                dto.tags() != null ? dto.tags() : List.of()
        );
    }
} 
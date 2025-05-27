package com.service.virtualization.kafka.dto;

/**
 * DTO for Schema Registry schema information
 */
public record SchemaInfoDTO(
    String subject,
    Integer id,
    Integer version,
    String schema
) {
    public String getSubject() {
        return subject;
    }

    public Integer getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public String getSchema() {
        return schema;
    }
} 
package com.service.virtualization.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for FileStub matching frontend requirements
 */
public record FileStubDTO(
    String id,
    String name,
    String description,
    String userId,
    String filePath,
    String content,
    String contentType,
    List<FileEntryDTO> files,
    String cronExpression,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // Default constructor with empty values
    public FileStubDTO() {
        this(null, null, null, null, null, null, null, List.of(), null, null, null, null);
    }
} 
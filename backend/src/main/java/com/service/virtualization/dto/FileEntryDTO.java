package com.service.virtualization.dto;

/**
 * Data Transfer Object for FileEntry matching frontend requirements
 */
public record FileEntryDTO(
    String filename,
    String contentType,
    String content
) {
    // Default constructor provides empty values
    public FileEntryDTO() {
        this("", "", "");
    }
} 
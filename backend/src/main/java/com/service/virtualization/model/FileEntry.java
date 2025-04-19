package com.service.virtualization.model;

import java.util.Objects;

/**
 * Represents an individual file entry within a FileStub
 */
public record FileEntry(
    String filename,
    String contentType,
    String content
) {
    public FileEntry {
        // Validate filename is provided
        Objects.requireNonNull(filename, "Filename cannot be null");
        if (filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        
        // Use empty string for null values
        contentType = contentType == null ? "" : contentType;
        content = content == null ? "" : content;
    }
    
    /**
     * Creates a default FileEntry with empty values
     * @param filename the filename (required)
     * @return a new FileEntry instance
     */
    public static FileEntry createDefault(String filename) {
        return new FileEntry(filename, "", "");
    }
} 
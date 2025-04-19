package com.service.virtualization.files;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a file stub in the system
 */
@Document(collection = "fileStubs")
public class FileStub {

    @Id
    private String id;
    
    /**
     * The name of the stub
     */
    private String name;
    
    /**
     * Optional description
     */
    private String description;
    
    /**
     * The user who created the stub
     */
    private String userId;
    
    /**
     * The base path for file serving
     */
    private String path;
    
    /**
     * Whether the stub is active
     */
    private boolean status;
    
    /**
     * List of files associated with this stub
     */
    private List<FileResource> files;
    
    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;
    
    public FileStub() {
        this.files = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public boolean isStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public List<FileResource> getFiles() {
        return files;
    }
    
    public void setFiles(List<FileResource> files) {
        this.files = files;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileStub fileStub = (FileStub) o;
        return status == fileStub.status && 
               Objects.equals(id, fileStub.id) && 
               Objects.equals(name, fileStub.name) && 
               Objects.equals(description, fileStub.description) && 
               Objects.equals(userId, fileStub.userId) && 
               Objects.equals(path, fileStub.path) && 
               Objects.equals(files, fileStub.files) && 
               Objects.equals(createdAt, fileStub.createdAt) && 
               Objects.equals(updatedAt, fileStub.updatedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, userId, path, status, files, createdAt, updatedAt);
    }
    
    @Override
    public String toString() {
        return "FileStub{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId='" + userId + '\'' +
                ", path='" + path + '\'' +
                ", status=" + status +
                ", files=" + files +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 
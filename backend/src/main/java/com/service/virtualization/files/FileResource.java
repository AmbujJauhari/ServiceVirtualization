package com.service.virtualization.files;

import org.springframework.data.annotation.Id;

import java.util.Objects;

/**
 * Represents a file resource in a file stub
 */
public class FileResource {

    @Id
    private String id;
    
    /**
     * The original filename
     */
    private String filename;
    
    /**
     * The content type of the file
     */
    private String contentType;
    
    /**
     * The size of the file in bytes
     */
    private long size;
    
    /**
     * The path where the file is stored
     */
    private String path;
    
    /**
     * Creation timestamp
     */
    private String createdAt;
    
    public FileResource() {
    }
    
    public FileResource(String id, String filename, String contentType, long size, String path, String createdAt) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.path = path;
        this.createdAt = createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileResource that = (FileResource) o;
        return size == that.size && 
               Objects.equals(id, that.id) && 
               Objects.equals(filename, that.filename) && 
               Objects.equals(contentType, that.contentType) && 
               Objects.equals(path, that.path) && 
               Objects.equals(createdAt, that.createdAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, filename, contentType, size, path, createdAt);
    }
    
    @Override
    public String toString() {
        return "FileResource{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
} 
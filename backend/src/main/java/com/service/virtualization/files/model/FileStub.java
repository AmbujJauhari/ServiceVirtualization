package com.service.virtualization.files.model;

import com.service.virtualization.model.StubStatus;
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
public record FileStub(
    @Id String id,
    String name,
    String description,
    String userId,
    String filePath,
    StubStatus status,
    String cronExpression,
    List<FileResource> files,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public FileStub {
        // Defensive copy for mutable list
        files = files != null ? List.copyOf(files) : List.of();
    }
    
    // Factory methods to handle immutability
    public FileStub withStatus(StubStatus newStatus) {
        return new FileStub(id, name, description, userId, filePath, newStatus, cronExpression, files, createdAt, LocalDateTime.now());
    }
    
    public FileStub withUpdatedAt() {
        return new FileStub(id, name, description, userId, filePath, status, cronExpression, files, createdAt, LocalDateTime.now());
    }
    
    public FileStub withCronExpression(String newCronExpression) {
        return new FileStub(id, name, description, userId, filePath, status, newCronExpression, files, createdAt, LocalDateTime.now());
    }
    
    public FileStub addFile(FileResource file) {
        List<FileResource> newFiles = new ArrayList<>(this.files);
        newFiles.add(file);
        return new FileStub(id, name, description, userId, filePath, status, cronExpression, newFiles, createdAt, LocalDateTime.now());
    }

    /**
     * Represents a file resource in a file stub
     */
    public static class FileResource {

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
         * The file path where the file is stored (if applicable)
         */
        private String path;

        /**
         * The actual content of the file
         */
        private String content;

        /**
         * Flag to indicate if timestamp should be added to filename
         */
        private boolean addTimestamp;

        /**
         * Creation timestamp
         */
        private String createdAt;

        public FileResource() {
            this.addTimestamp = false;
        }

        public FileResource(String id, String filename, String contentType, long size, String path, String content, boolean addTimestamp, String createdAt) {
            this.id = id;
            this.filename = filename;
            this.contentType = contentType;
            this.size = size;
            this.path = path;
            this.content = content;
            this.addTimestamp = addTimestamp;
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

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isAddTimestamp() {
            return addTimestamp;
        }

        public void setAddTimestamp(boolean addTimestamp) {
            this.addTimestamp = addTimestamp;
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
                   addTimestamp == that.addTimestamp &&
                   Objects.equals(id, that.id) &&
                   Objects.equals(filename, that.filename) &&
                   Objects.equals(contentType, that.contentType) &&
                   Objects.equals(path, that.path) &&
                   Objects.equals(content, that.content) &&
                   Objects.equals(createdAt, that.createdAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, filename, contentType, size, path, content, addTimestamp, createdAt);
        }

        @Override
        public String toString() {
            return "FileResource{" +
                    "id='" + id + '\'' +
                    ", filename='" + filename + '\'' +
                    ", contentType='" + contentType + '\'' +
                    ", size=" + size +
                    ", path='" + path + '\'' +
                    ", content='" + (content != null ? "[content length: " + content.length() + "]" : "null") + '\'' +
                    ", addTimestamp=" + addTimestamp +
                    ", createdAt='" + createdAt + '\'' +
                    '}';
        }
    }
}
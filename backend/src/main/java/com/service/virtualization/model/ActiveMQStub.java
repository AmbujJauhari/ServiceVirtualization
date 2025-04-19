package com.service.virtualization.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an ActiveMQ message stub for service virtualization
 */
@Document(collection = "activemq_stubs")
public class ActiveMQStub {

    /**
     * Unique identifier for the stub
     */
    @Id
    private String id;
    
    /**
     * Name of the stub
     */
    private String name;
    
    /**
     * Description of the stub
     */
    private String description;
    
    /**
     * User ID that owns this stub
     */
    private String userId;
    
    /**
     * Type of the destination (queue or topic)
     */
    private String destinationType;
    
    /**
     * Name of the destination
     */
    private String destinationName;
    
    /**
     * JMS selector for filtering messages
     */
    private String selector;
    
    /**
     * Content of the response message
     */
    private String responseContent;
    
    /**
     * Type of the response (text, json, xml, bytes)
     */
    private String responseType;
    
    /**
     * Artificial latency in milliseconds
     */
    private Integer latency;
    
    /**
     * Custom message headers to include in the response
     */
    private List<MessageHeader> headers;
    
    /**
     * Status of the stub (active/inactive)
     */
    private boolean status;
    
    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;
    
    /**
     * Default constructor
     */
    public ActiveMQStub() {
        this.headers = new ArrayList<>();
        this.latency = 0;
        this.status = false;
    }
    
    /**
     * Constructor with essential fields
     */
    public ActiveMQStub(String name, String destinationType, String destinationName, String userId) {
        this();
        this.name = name;
        this.destinationType = destinationType;
        this.destinationName = destinationName;
        this.userId = userId;
    }

    // Getters and Setters
    
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

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public Integer getLatency() {
        return latency;
    }

    public void setLatency(Integer latency) {
        this.latency = latency;
    }

    public List<MessageHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<MessageHeader> headers) {
        this.headers = headers;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
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
    
    // Utility methods
    
    /**
     * Add a header to the list of headers
     * @param header the header to add
     */
    public void addHeader(MessageHeader header) {
        if (this.headers == null) {
            this.headers = new ArrayList<>();
        }
        this.headers.add(header);
    }
    
    /**
     * Remove a header by name
     * @param headerName the name of the header to remove
     * @return true if a header was removed, false otherwise
     */
    public boolean removeHeader(String headerName) {
        if (this.headers == null) {
            return false;
        }
        return this.headers.removeIf(h -> Objects.equals(h.getName(), headerName));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveMQStub that = (ActiveMQStub) o;
        return status == that.status &&
               Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(destinationType, that.destinationType) &&
               Objects.equals(destinationName, that.destinationName) &&
               Objects.equals(selector, that.selector) &&
               Objects.equals(responseContent, that.responseContent) &&
               Objects.equals(responseType, that.responseType) &&
               Objects.equals(latency, that.latency) &&
               Objects.equals(headers, that.headers) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, userId, destinationType, destinationName, 
                           selector, responseContent, responseType, latency, headers, 
                           status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "ActiveMQStub{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId='" + userId + '\'' +
                ", destinationType='" + destinationType + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", selector='" + selector + '\'' +
                ", responseType='" + responseType + '\'' +
                ", status=" + status +
                '}';
    }
} 
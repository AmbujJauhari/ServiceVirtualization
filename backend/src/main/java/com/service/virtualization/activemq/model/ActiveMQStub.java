package com.service.virtualization.activemq.model;

import com.service.virtualization.model.StubStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing an ActiveMQ stub configuration.
 */
@Document(collection = "activemq_stubs")
public class ActiveMQStub {
    
    /**
     * Enum defining the types of content matching available.
     */
    public enum ContentMatchType {
        NONE,       // No content matching
        CONTAINS,   // Message content contains the pattern
        EXACT,      // Message content exactly matches the pattern
        REGEX       // Message content matches the regex pattern
    }
    
    @Id
    private String id;
    
    private String name;
    private String description;
    private String userId;
    
    // Request configuration
    private String destinationType; // "queue" or "topic"
    private String destinationName;
    private String messageSelector;
    
    // Content matching configuration
    private ContentMatchType contentMatchType = ContentMatchType.NONE;
    private String contentPattern;
    private boolean caseSensitive = true;
    
    // Response configuration
    private String responseType; // "queue" or "topic"
    private String responseDestination;
    private String responseContent;
    
    // Webhook configuration
    private String webhookUrl;
    
    // Custom headers for response
    private Map<String, String> headers = new HashMap<>();
    
    // Status (enabled/disabled)
    private StubStatus status = StubStatus.ACTIVE;
    
    // Auditing fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Default constructor.
     */
    public ActiveMQStub() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor with required fields.
     */
    public ActiveMQStub(String name, String destinationType, String destinationName, 
                       String responseType, String responseDestination, String responseContent) {
        this();
        this.name = name;
        this.destinationType = destinationType;
        this.destinationName = destinationName;
        this.responseType = responseType;
        this.responseDestination = responseDestination;
        this.responseContent = responseContent;
    }
    
    // Getters and setters
    
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
    
    public String getMessageSelector() {
        return messageSelector;
    }
    
    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }
    
    public ContentMatchType getContentMatchType() {
        return contentMatchType;
    }
    
    public void setContentMatchType(ContentMatchType contentMatchType) {
        this.contentMatchType = contentMatchType;
    }
    
    public String getContentPattern() {
        return contentPattern;
    }
    
    public void setContentPattern(String contentPattern) {
        this.contentPattern = contentPattern;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public String getResponseDestination() {
        return responseDestination;
    }
    
    public void setResponseDestination(String responseDestination) {
        this.responseDestination = responseDestination;
    }
    
    public String getResponseContent() {
        return responseContent;
    }
    
    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }
    
    public String getResponseDestinationType() {
        return responseType; // Alias for responseType
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public void addHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(name, value);
    }
    
    public StubStatus getStatus() {
        return status;
    }
    
    public void setStatus(StubStatus status) {
        this.status = status;
    }
    
    public boolean isActive() {
        return status == StubStatus.ACTIVE;
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
    public String toString() {
        return "ActiveMQStub{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", destinationType='" + destinationType + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", contentMatchType=" + contentMatchType +
                ", responseDestination='" + responseDestination + '\'' +
                ", status=" + status +
                '}';
    }
} 
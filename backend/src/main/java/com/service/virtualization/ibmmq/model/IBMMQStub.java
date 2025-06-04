package com.service.virtualization.ibmmq.model;

import com.service.virtualization.model.StubStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an IBM MQ message stub for service virtualization
 */
@Document(collection = "ibmmq_stubs")
public class IBMMQStub {

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
    private boolean caseSensitive = false;

    // Response configuration
    private String responseType; // "queue" or "topic"
    private String responseDestination;
    private String responseContent;

    // Webhook configuration
    private String webhookUrl;

    // Priority for stub matching (higher number = higher priority)
    private int priority = 0;

    private Integer latency;

    // Custom headers for response
    private Map<String, String> headers = new HashMap<>();
    
    private StubStatus status = StubStatus.ACTIVE;;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    /**
     * Default constructor
     */
    public IBMMQStub() {
        this.latency = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getLatency() {
        return latency;
    }

    public void setLatency(Integer latency) {
        this.latency = latency;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public StubStatus getStatus() {
        return status;
    }

    public void setStatus(StubStatus status) {
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

    public boolean isActive() {
        return status == StubStatus.ACTIVE;
    }
}
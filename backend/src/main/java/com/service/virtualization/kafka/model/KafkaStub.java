package com.service.virtualization.kafka.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Domain model for Kafka stubs
 */
@Document(collection = "kafka_stubs")
public class KafkaStub {
    @Id
    private String id;
    private String name;
    private String description;
    private String userId;
    private String requestTopic;
    private String responseTopic;
    
    // Content formats and matching
    private String requestContentFormat;
    private String responseContentFormat;
    private String requestContentMatcher;
    
    // Key matching
    private String keyMatchType;
    private String keyPattern;
    
    // Content/Value matching
    private String contentMatchType;
    private String valuePattern; // Legacy field
    private String contentPattern; // New field name
    private Boolean caseSensitive;
    
    // Response configuration
    private String responseType; // 'direct' or 'callback'
    private String responseKey;
    private String responseContent;
    
    // Schema Registry for response validation (transient/UI-only)
    private transient Boolean useResponseSchemaRegistry;
    private transient String responseSchemaId;
    private transient String responseSchemaSubject;
    private transient String responseSchemaVersion;
    
    private Integer latency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Callback configuration
    private String callbackUrl;
    private Map<String, String> callbackHeaders;

    public KafkaStub() {
        // Default constructor
    }

    public KafkaStub(String id, String name, String description, String userId, String requestTopic, String responseTopic, String keyPattern,
                     String valuePattern, String responseType, String responseKey, String responseContent, Integer latency, String status, 
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.requestTopic = requestTopic;
        this.responseTopic = responseTopic;
        this.keyPattern = keyPattern;
        this.valuePattern = valuePattern;
        this.responseType = responseType;
        this.responseKey = responseKey;
        this.responseContent = responseContent;
        this.latency = latency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getRequestTopic() {
        return requestTopic;
    }

    public void setRequestTopic(String requestTopic) {
        this.requestTopic = requestTopic;
    }

    public String getResponseTopic() {
        return responseTopic;
    }

    public void setResponseTopic(String responseTopic) {
        this.responseTopic = responseTopic;
    }

    public String getKeyPattern() {
        return keyPattern;
    }

    public void setKeyPattern(String keyPattern) {
        this.keyPattern = keyPattern;
    }

    public String getValuePattern() {
        return valuePattern;
    }

    public void setValuePattern(String valuePattern) {
        this.valuePattern = valuePattern;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getResponseKey() {
        return responseKey;
    }

    public void setResponseKey(String responseKey) {
        this.responseKey = responseKey;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    public Integer getLatency() {
        return latency;
    }

    public void setLatency(Integer latency) {
        this.latency = latency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Map<String, String> getCallbackHeaders() {
        return callbackHeaders;
    }

    public void setCallbackHeaders(Map<String, String> callbackHeaders) {
        this.callbackHeaders = callbackHeaders;
    }

    // Content formats and matching
    public String getRequestContentFormat() {
        return requestContentFormat;
    }

    public void setRequestContentFormat(String requestContentFormat) {
        this.requestContentFormat = requestContentFormat;
    }

    public String getResponseContentFormat() {
        return responseContentFormat;
    }

    public void setResponseContentFormat(String responseContentFormat) {
        this.responseContentFormat = responseContentFormat;
    }

    public String getRequestContentMatcher() {
        return requestContentMatcher;
    }

    public void setRequestContentMatcher(String requestContentMatcher) {
        this.requestContentMatcher = requestContentMatcher;
    }

    // Key matching
    public String getKeyMatchType() {
        return keyMatchType;
    }

    public void setKeyMatchType(String keyMatchType) {
        this.keyMatchType = keyMatchType;
    }

    // Content/Value matching
    public String getContentMatchType() {
        return contentMatchType;
    }

    public void setContentMatchType(String contentMatchType) {
        this.contentMatchType = contentMatchType;
    }

    public String getContentPattern() {
        return contentPattern;
    }

    public void setContentPattern(String contentPattern) {
        this.contentPattern = contentPattern;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    // Schema Registry fields (transient/UI-only)
    public Boolean getUseResponseSchemaRegistry() {
        return useResponseSchemaRegistry;
    }

    public void setUseResponseSchemaRegistry(Boolean useResponseSchemaRegistry) {
        this.useResponseSchemaRegistry = useResponseSchemaRegistry;
    }

    public String getResponseSchemaId() {
        return responseSchemaId;
    }

    public void setResponseSchemaId(String responseSchemaId) {
        this.responseSchemaId = responseSchemaId;
    }

    public String getResponseSchemaSubject() {
        return responseSchemaSubject;
    }

    public void setResponseSchemaSubject(String responseSchemaSubject) {
        this.responseSchemaSubject = responseSchemaSubject;
    }

    public String getResponseSchemaVersion() {
        return responseSchemaVersion;
    }

    public void setResponseSchemaVersion(String responseSchemaVersion) {
        this.responseSchemaVersion = responseSchemaVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaStub kafkaStub = (KafkaStub) o;
        return Objects.equals(id, kafkaStub.id) &&
                Objects.equals(name, kafkaStub.name) &&
                Objects.equals(description, kafkaStub.description) &&
                Objects.equals(userId, kafkaStub.userId) &&
                Objects.equals(requestTopic, kafkaStub.requestTopic) &&
                Objects.equals(responseTopic, kafkaStub.responseTopic) &&
                Objects.equals(keyPattern, kafkaStub.keyPattern) &&
                Objects.equals(valuePattern, kafkaStub.valuePattern) &&
                Objects.equals(responseType, kafkaStub.responseType) &&
                Objects.equals(responseKey, kafkaStub.responseKey) &&
                Objects.equals(responseContent, kafkaStub.responseContent) &&
                Objects.equals(latency, kafkaStub.latency) &&
                Objects.equals(status, kafkaStub.status) &&
                Objects.equals(createdAt, kafkaStub.createdAt) &&
                Objects.equals(updatedAt, kafkaStub.updatedAt) &&
                Objects.equals(callbackUrl, kafkaStub.callbackUrl) &&
                Objects.equals(callbackHeaders, kafkaStub.callbackHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, userId, requestTopic, responseTopic, keyPattern, valuePattern,
                responseType, responseKey, responseContent, latency, status, createdAt, updatedAt, callbackUrl, callbackHeaders);
    }

    @Override
    public String toString() {
        return "KafkaStub{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId='" + userId + '\'' +
                ", requestTopic='" + requestTopic + '\'' +
                ", responseTopic='" + responseTopic + '\'' +
                ", keyPattern='" + keyPattern + '\'' +
                ", valuePattern='" + valuePattern + '\'' +
                ", responseType='" + responseType + '\'' +
                ", responseKey='" + responseKey + '\'' +
                ", responseContent='" + responseContent + '\'' +
                ", latency=" + latency +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", callbackUrl='" + callbackUrl + '\'' +
                ", callbackHeaders=" + callbackHeaders +
                '}';
    }
} 
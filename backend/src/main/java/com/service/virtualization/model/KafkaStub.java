package com.service.virtualization.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Domain model for Kafka stubs
 */
public class KafkaStub {
    private Long id;
    private String name;
    private String description;
    private String userId;
    private String topic;
    private String keyPattern;
    private String valuePattern;
    private Boolean activeForProducer;
    private Boolean activeForConsumer;
    private String responseType; // 'direct' or 'callback'
    private String responseContent;
    private Integer latency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public KafkaStub() {
        // Default constructor
    }

    public KafkaStub(Long id, String name, String description, String userId, String topic, String keyPattern,
                     String valuePattern, Boolean activeForProducer, Boolean activeForConsumer, String responseType,
                     String responseContent, Integer latency, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.topic = topic;
        this.keyPattern = keyPattern;
        this.valuePattern = valuePattern;
        this.activeForProducer = activeForProducer;
        this.activeForConsumer = activeForConsumer;
        this.responseType = responseType;
        this.responseContent = responseContent;
        this.latency = latency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public Boolean getActiveForProducer() {
        return activeForProducer;
    }

    public void setActiveForProducer(Boolean activeForProducer) {
        this.activeForProducer = activeForProducer;
    }

    public Boolean getActiveForConsumer() {
        return activeForConsumer;
    }

    public void setActiveForConsumer(Boolean activeForConsumer) {
        this.activeForConsumer = activeForConsumer;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaStub kafkaStub = (KafkaStub) o;
        return Objects.equals(id, kafkaStub.id) &&
                Objects.equals(name, kafkaStub.name) &&
                Objects.equals(description, kafkaStub.description) &&
                Objects.equals(userId, kafkaStub.userId) &&
                Objects.equals(topic, kafkaStub.topic) &&
                Objects.equals(keyPattern, kafkaStub.keyPattern) &&
                Objects.equals(valuePattern, kafkaStub.valuePattern) &&
                Objects.equals(activeForProducer, kafkaStub.activeForProducer) &&
                Objects.equals(activeForConsumer, kafkaStub.activeForConsumer) &&
                Objects.equals(responseType, kafkaStub.responseType) &&
                Objects.equals(responseContent, kafkaStub.responseContent) &&
                Objects.equals(latency, kafkaStub.latency) &&
                Objects.equals(status, kafkaStub.status) &&
                Objects.equals(createdAt, kafkaStub.createdAt) &&
                Objects.equals(updatedAt, kafkaStub.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, userId, topic, keyPattern, valuePattern,
                activeForProducer, activeForConsumer, responseType, responseContent, latency, status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "KafkaStub{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId='" + userId + '\'' +
                ", topic='" + topic + '\'' +
                ", keyPattern='" + keyPattern + '\'' +
                ", valuePattern='" + valuePattern + '\'' +
                ", activeForProducer=" + activeForProducer +
                ", activeForConsumer=" + activeForConsumer +
                ", responseType='" + responseType + '\'' +
                ", responseContent='" + responseContent + '\'' +
                ", latency=" + latency +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 
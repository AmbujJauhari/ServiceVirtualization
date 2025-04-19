package com.service.virtualization.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a TIBCO EMS stub.
 */
public class TibcoStub {
    private String id;
    private String name;
    private String description;
    private String userId;
    private TibcoDestination requestDestination;
    private TibcoDestination responseDestination;
    private String messageSelector;
    private List<BodyMatchCriteria> bodyMatchCriteria;
    private String responseType; // 'direct' or 'callback'
    private String responseContent;
    private Map<String, String> responseHeaders;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TibcoStub() {
        this.responseHeaders = new HashMap<>();
        this.bodyMatchCriteria = new ArrayList<>();
    }

    public TibcoStub(String id, String name, String description, String userId, TibcoDestination requestDestination, TibcoDestination responseDestination,
                    String messageSelector, String responseType, String responseContent, Map<String, String> responseHeaders, Integer latency,
                    String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.requestDestination = requestDestination;
        this.responseDestination = responseDestination;
        this.messageSelector = messageSelector;
        this.responseType = responseType;
        this.responseContent = responseContent;
        this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public TibcoDestination getRequestDestination() {
        return requestDestination;
    }

    public void setRequestDestination(TibcoDestination requestDestination) {
        this.requestDestination = requestDestination;
    }

    public TibcoDestination getResponseDestination() {
        return responseDestination;
    }

    public void setResponseDestination(TibcoDestination responseDestination) {
        this.responseDestination = responseDestination;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
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

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
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

    public List<BodyMatchCriteria> getBodyMatchCriteria() {
        return bodyMatchCriteria;
    }

    public void setBodyMatchCriteria(List<BodyMatchCriteria> bodyMatchCriteria) {
        this.bodyMatchCriteria = bodyMatchCriteria != null ? bodyMatchCriteria : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TibcoStub tibcoStub = (TibcoStub) o;
        return Objects.equals(id, tibcoStub.id) && 
               Objects.equals(name, tibcoStub.name) && 
               Objects.equals(description, tibcoStub.description) && 
               Objects.equals(userId, tibcoStub.userId) && 
               Objects.equals(requestDestination, tibcoStub.requestDestination) &&
               Objects.equals(messageSelector, tibcoStub.messageSelector) && 
               Objects.equals(responseType, tibcoStub.responseType) && 
               Objects.equals(responseContent, tibcoStub.responseContent) && 
               Objects.equals(responseHeaders, tibcoStub.responseHeaders) &&
               Objects.equals(status, tibcoStub.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, userId, requestDestination, responseDestination, messageSelector,
                           responseType, responseContent, responseHeaders, status);
    }

    @Override
    public String toString() {
        return "TibcoStub{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId='" + userId + '\'' +
                ", destination=" + requestDestination +
                ", messageSelector='" + messageSelector + '\'' +
                ", responseType='" + responseType + '\'' +
                ", responseContent='" + responseContent + '\'' +
                ", responseHeaders='" + responseHeaders + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Inner class for body match criteria
    public static class BodyMatchCriteria {
        private String type; // "xpath", "jsonpath"
        private String expression;
        private String value;
        private String operator; // "equals", "contains", "startsWith", "endsWith", "regex"

        public BodyMatchCriteria() {
        }

        public BodyMatchCriteria(String type, String expression, String value, String operator) {
            this.type = type;
            this.expression = expression;
            this.value = value;
            this.operator = operator;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BodyMatchCriteria that = (BodyMatchCriteria) o;
            return Objects.equals(type, that.type) &&
                   Objects.equals(expression, that.expression) &&
                   Objects.equals(value, that.value) &&
                   Objects.equals(operator, that.operator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, expression, value, operator);
        }
    }
} 
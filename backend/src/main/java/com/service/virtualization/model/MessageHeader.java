package com.service.virtualization.model;

import java.util.Objects;

/**
 * Represents a message header/property for ActiveMQ messages
 */
public class MessageHeader {

    /**
     * Header name
     */
    private String name;
    
    /**
     * Header value
     */
    private String value;
    
    /**
     * Header type (String, Integer, Boolean, etc.)
     */
    private String type;
    
    public MessageHeader() {
    }
    
    public MessageHeader(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageHeader that = (MessageHeader) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(type, that.type);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, value, type);
    }
    
    @Override
    public String toString() {
        return "MessageHeader{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
} 
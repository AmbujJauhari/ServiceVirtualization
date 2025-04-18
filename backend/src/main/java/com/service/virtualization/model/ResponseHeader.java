package com.service.virtualization.model;

import java.util.Objects;

/**
 * Represents a response header to return in the stub response
 */
public class ResponseHeader {
    
    private String name;
    private String value;
    
    // Default constructor
    public ResponseHeader() {
    }
    
    // Parameterized constructor
    public ResponseHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    // Getters and Setters
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseHeader that = (ResponseHeader) o;
        return Objects.equals(name, that.name) && 
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
    
    @Override
    public String toString() {
        return "ResponseHeader{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
} 
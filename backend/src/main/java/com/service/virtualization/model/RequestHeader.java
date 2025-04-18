package com.service.virtualization.model;

import java.util.Objects;

/**
 * Represents a request header to match in the stub
 */
public class RequestHeader {
    
    private String name;
    private String value;
    private boolean exactMatch = true;
    
    // Default constructor
    public RequestHeader() {
    }
    
    // Parameterized constructor
    public RequestHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    public RequestHeader(String name, String value, boolean exactMatch) {
        this.name = name;
        this.value = value;
        this.exactMatch = exactMatch;
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
    
    public boolean isExactMatch() {
        return exactMatch;
    }
    
    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestHeader that = (RequestHeader) o;
        return exactMatch == that.exactMatch && 
               Objects.equals(name, that.name) && 
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, value, exactMatch);
    }
    
    @Override
    public String toString() {
        return "RequestHeader{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", exactMatch=" + exactMatch +
                '}';
    }
} 
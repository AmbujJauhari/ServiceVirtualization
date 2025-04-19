package com.service.virtualization.tibco;

import java.util.Objects;

/**
 * Model class representing a TIBCO EMS destination.
 */
public class TibcoDestination {
    private String type; // QUEUE or TOPIC
    private String name;

    public TibcoDestination() {
    }

    public TibcoDestination(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TibcoDestination that = (TibcoDestination) o;
        return Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return "TibcoDestination{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
} 
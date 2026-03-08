package com.service.virtualization.activemq.config;

/**
 * Configuration for a single ActiveMQ server.
 * Supports multi-server ActiveMQ configuration.
 */
public class ActiveMqServerConfig {
    private final String name;
    private final String brokerUrl;
    private final String username;
    private final String password;
    private final Integer timeout;
    private final Boolean enabled;
    
    public ActiveMqServerConfig(String name, String brokerUrl, String username, 
                               String password, Integer timeout, Boolean enabled) {
        this.name = name;
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.enabled = enabled != null ? enabled : true;
    }
    
    public String getName() { return name; }
    public String getBrokerUrl() { return brokerUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Integer getTimeout() { return timeout; }
    public Boolean getEnabled() { return enabled; }
    
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("ActiveMQ server name is required");
        }
        if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
            throw new IllegalStateException("ActiveMQ broker URL is required for server: " + name);
        }
        if (!brokerUrl.startsWith("tcp://") && !brokerUrl.startsWith("ssl://") && 
            !brokerUrl.startsWith("nio://") && !brokerUrl.startsWith("failover://")) {
            throw new IllegalStateException(
                "ActiveMQ broker URL must start with a valid protocol (tcp://, ssl://, nio://, failover://) for server: " + name + 
                ", got: " + brokerUrl
            );
        }
    }
    
    @Override
    public String toString() {
        return "ActiveMqServerConfig{" +
                "name='" + name + '\'' +
                ", brokerUrl='" + brokerUrl + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" +
                ", timeout=" + timeout +
                ", enabled=" + enabled +
                '}';
    }
} 
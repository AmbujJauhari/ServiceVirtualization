package com.service.virtualization.ibmmq.config;

import java.util.Objects;

/**
 * Configuration for a single IBM MQ server.
 * Supports multi-server IBM MQ configuration.
 */
public class IbmMqServerConfig {
    private final String name;
    private final String host;
    private final Integer port;
    private final String queueManager;
    private final String channel;
    private final String username;
    private final String password;
    private final Integer timeout;
    private final Boolean enabled;
    private final boolean sslEnabled;
    // Cipher suite must match the SSLCIPH configured on the MQ server-connection channel.
    // Default matches the channel default set in ibmmq-*-values.yaml MQSC config.
    private final String sslCipherSuite;

    public IbmMqServerConfig(String name, String host, Integer port, String queueManager,
                            String channel, String username, String password,
                            Integer timeout, Boolean enabled,
                            boolean sslEnabled, String sslCipherSuite) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.queueManager = queueManager;
        this.channel = channel;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.enabled = enabled != null ? enabled : true;
        this.sslEnabled = sslEnabled;
        this.sslCipherSuite = (sslCipherSuite != null && !sslCipherSuite.trim().isEmpty())
                ? sslCipherSuite : "TLS_RSA_WITH_AES_256_CBC_SHA256";
    }

    public String getName() { return name; }
    public String getHost() { return host; }
    public Integer getPort() { return port; }
    public String getQueueManager() { return queueManager; }
    public String getChannel() { return channel; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Integer getTimeout() { return timeout; }
    public Boolean getEnabled() { return enabled; }
    public boolean isSslEnabled() { return sslEnabled; }
    public String getSslCipherSuite() { return sslCipherSuite; }
    
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("IBM MQ server name is required");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalStateException("IBM MQ host is required for server: " + name);
        }
        if (port == null || port <= 0) {
            throw new IllegalStateException("IBM MQ port is required for server: " + name);
        }
        if (queueManager == null || queueManager.trim().isEmpty()) {
            throw new IllegalStateException("IBM MQ queue manager is required for server: " + name);
        }
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalStateException("IBM MQ channel is required for server: " + name);
        }
    }
    
    @Override
    public String toString() {
        return "IbmMqServerConfig{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", queueManager='" + queueManager + '\'' +
                ", channel='" + channel + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" +
                ", timeout=" + timeout +
                ", enabled=" + enabled +
                ", sslEnabled=" + sslEnabled +
                ", sslCipherSuite='" + sslCipherSuite + '\'' +
                '}';
    }
} 
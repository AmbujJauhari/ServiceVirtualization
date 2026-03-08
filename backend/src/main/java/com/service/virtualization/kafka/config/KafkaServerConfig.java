package com.service.virtualization.kafka.config;

/**
 * Configuration for a single Kafka cluster.
 * Supports multi-cluster Kafka configuration.
 */
public class KafkaServerConfig {
    private final String name;
    private final String bootstrapServers;
    private final String consumerGroupId;
    private final Integer timeout;
    private final Boolean enabled;
    private final boolean sslEnabled;
    private final String truststorePath;
    private final String truststorePassword;
    // Empty string disables hostname verification — required when the wildcard cert
    // (*.service-virtualization.local) does not cover internal k8s service hostnames.
    private final String endpointIdentificationAlgorithm;

    public KafkaServerConfig(String name, String bootstrapServers, String consumerGroupId,
                            Integer timeout, Boolean enabled,
                            boolean sslEnabled, String truststorePath, String truststorePassword,
                            String endpointIdentificationAlgorithm) {
        this.name = name;
        this.bootstrapServers = bootstrapServers;
        this.consumerGroupId = consumerGroupId;
        this.timeout = timeout;
        this.enabled = enabled != null ? enabled : true;
        this.sslEnabled = sslEnabled;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.endpointIdentificationAlgorithm = endpointIdentificationAlgorithm != null
                ? endpointIdentificationAlgorithm : "";
    }

    public String getName() { return name; }
    public String getBootstrapServers() { return bootstrapServers; }
    public String getConsumerGroupId() { return consumerGroupId; }
    public Integer getTimeout() { return timeout; }
    public Boolean getEnabled() { return enabled; }
    public boolean isSslEnabled() { return sslEnabled; }
    public String getTruststorePath() { return truststorePath; }
    public String getTruststorePassword() { return truststorePassword; }
    public String getEndpointIdentificationAlgorithm() { return endpointIdentificationAlgorithm; }
    
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Kafka cluster name is required");
        }
        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            throw new IllegalStateException("Kafka bootstrap servers are required for cluster: " + name);
        }
        if (consumerGroupId == null || consumerGroupId.trim().isEmpty()) {
            throw new IllegalStateException("Kafka consumer group ID is required for cluster: " + name);
        }
    }
    
    @Override
    public String toString() {
        return "KafkaServerConfig{" +
                "name='" + name + '\'' +
                ", bootstrapServers='" + bootstrapServers + '\'' +
                ", consumerGroupId='" + consumerGroupId + '\'' +
                ", timeout=" + timeout +
                ", enabled=" + enabled +
                ", sslEnabled=" + sslEnabled +
                ", truststorePath='" + truststorePath + '\'' +
                '}';
    }
} 
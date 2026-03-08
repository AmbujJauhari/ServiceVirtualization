package com.service.virtualization.kafka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Registry for multiple Kafka cluster configurations.
 * Discovers cluster configurations from environment properties at application startup.
 * 
 * Cluster properties should follow the pattern:
 * {@code kafka.registry.<clusterName>.<property>}
 * 
 * Example:
 * <pre>
 * KAFKA_REGISTRY_CLUSTERA_BOOTSTRAP_SERVERS=kafka-a1.company.com:9092,kafka-a2.company.com:9092
 * KAFKA_REGISTRY_CLUSTERA_CONSUMER_GROUP_ID=sv-cluster-a
 * </pre>
 */
@Component
@Profile("!kafka-disabled")
public class KafkaServerRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaServerRegistry.class);
    private static final String REGISTRY_PREFIX = "kafka.registry.";
    private static final Pattern CLUSTER_NAME_PATTERN = Pattern.compile("^kafka\\.registry\\.([^.]+)\\..*");
    
    private final Environment environment;
    private final Map<String, KafkaServerConfig> clusterConfigs = new ConcurrentHashMap<>();
    
    @Autowired
    public KafkaServerRegistry(Environment environment) {
        this.environment = environment;
    }
    
    @PostConstruct
    public void initialize() {
        log.info("🔧 Initializing Kafka Cluster Registry...");
        
        Set<String> clusterNames = discoverClusterNames();
        
        if (clusterNames.isEmpty()) {
            log.warn("⚠️  No Kafka clusters configured (kafka.registry.*). Kafka functionality will be unavailable.");
            return;
        }
        
        log.info("📋 Discovered {} Kafka cluster(s): {}", clusterNames.size(), clusterNames);
        
        for (String clusterName : clusterNames) {
            try {
                KafkaServerConfig config = loadClusterConfig(clusterName);
                config.validate();
                clusterConfigs.put(clusterName, config);
                log.info("✅ Registered Kafka cluster '{}': {} (Group: {})", 
                         clusterName, config.getBootstrapServers(), config.getConsumerGroupId());
            } catch (IllegalStateException e) {
                log.error("❌ Failed to register Kafka cluster '{}': {}", clusterName, e.getMessage());
                throw new IllegalStateException("Invalid Kafka cluster configuration for '" + clusterName + "': " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("❌ Unexpected error during Kafka cluster '{}' initialization: {}", clusterName, e.getMessage(), e);
                throw new IllegalStateException("Unexpected error during Kafka cluster '" + clusterName + "' initialization: " + e.getMessage(), e);
            }
        }
        
        log.info("🎉 Kafka Cluster Registry initialized successfully with {} cluster(s)", clusterConfigs.size());
    }
    
    private Set<String> discoverClusterNames() {
        Set<String> clusterNames = ConcurrentHashMap.newKeySet();
        
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            configurableEnv.getPropertySources().forEach(propertySource -> {
                if (propertySource instanceof org.springframework.core.env.EnumerablePropertySource) {
                    org.springframework.core.env.EnumerablePropertySource<?> enumerablePropertySource = 
                        (org.springframework.core.env.EnumerablePropertySource<?>) propertySource;
                    
                    for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                        if (propertyName.startsWith(REGISTRY_PREFIX)) {
                            Matcher matcher = CLUSTER_NAME_PATTERN.matcher(propertyName);
                            if (matcher.matches()) {
                                clusterNames.add(matcher.group(1));
                            }
                        }
                    }
                }
            });
        }
        return clusterNames;
    }
    
    private KafkaServerConfig loadClusterConfig(String clusterName) {
        String prefix = REGISTRY_PREFIX + clusterName + ".";

        String bootstrapServers = getRequiredProperty(prefix + "bootstrap.servers", clusterName);
        String consumerGroupId = getProperty(prefix + "consumer.group.id", "sv-" + clusterName);
        Integer timeout = getIntegerProperty(prefix + "timeout", 30000);
        Boolean enabled = getBooleanProperty(prefix + "enabled", true);

        // SSL — env vars: KAFKA_REGISTRY_<NAME>_SSL_ENABLED, _SSL_TRUSTSTORE_PATH, _SSL_TRUSTSTORE_PASSWORD
        Boolean sslEnabled = getBooleanProperty(prefix + "ssl.enabled", false);
        String truststorePath = getProperty(prefix + "ssl.truststore.path", null);
        String truststorePassword = getProperty(prefix + "ssl.truststore.password", null);
        // Default "" disables hostname verification — required for wildcard certs on internal hostnames
        String endpointIdAlg = getProperty(prefix + "ssl.endpoint.identification.algorithm", "");

        return new KafkaServerConfig(clusterName, bootstrapServers, consumerGroupId, timeout, enabled,
                                     sslEnabled, truststorePath, truststorePassword, endpointIdAlg);
    }
    
    private String getRequiredProperty(String propertyName, String clusterName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "Required Kafka property '" + propertyName + "' is missing or empty for cluster: " + clusterName
            );
        }
        return value;
    }
    
    private String getProperty(String propertyName, String defaultValue) {
        String value = environment.getProperty(propertyName);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    private Integer getIntegerProperty(String propertyName, Integer defaultValue) {
        String value = environment.getProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer format for property '{}': {}. Using default value: {}", 
                        propertyName, value, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private Boolean getBooleanProperty(String propertyName, Boolean defaultValue) {
        String value = environment.getProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    public KafkaServerConfig getClusterConfig(String clusterName) {
        KafkaServerConfig config = clusterConfigs.get(clusterName);
        if (config == null) {
            throw new IllegalArgumentException("Kafka cluster '" + clusterName + "' not found in registry.");
        }
        return config;
    }
    
    public boolean hasCluster(String clusterName) {
        return clusterConfigs.containsKey(clusterName);
    }
    
    public List<String> getAvailableClusterNames() {
        return Collections.unmodifiableList(clusterConfigs.keySet().stream().sorted().collect(Collectors.toList()));
    }
    
    public Map<String, KafkaServerConfig> getAllClusters() {
        return Collections.unmodifiableMap(clusterConfigs);
    }
    
    public int getClusterCount() {
        return clusterConfigs.size();
    }
    
    public boolean isEmpty() {
        return clusterConfigs.isEmpty();
    }
} 
package com.service.virtualization.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing multiple Kafka producer factories and templates.
 * 
 * Creates and maintains KafkaTemplate instances for each configured Kafka cluster.
 */
@Component
@Profile("!kafka-disabled")
public class KafkaProducerFactoryRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerFactoryRegistry.class);
    
    @Autowired
    private KafkaServerRegistry serverRegistry;
    
    private final Map<String, KafkaTemplate<String, String>> kafkaTemplates = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("🔧 Initializing Kafka Producer Factory Registry...");

        try {
            if (serverRegistry.isEmpty()) {
                log.warn("⚠️  No Kafka clusters configured (kafka.registry.*). Kafka producing will be unavailable.");
                return;
            }

            for (Map.Entry<String, KafkaServerConfig> entry : serverRegistry.getAllClusters().entrySet()) {
                String clusterName = entry.getKey();
                KafkaServerConfig config = entry.getValue();

                if (!config.getEnabled()) {
                    log.info("⏭️  Skipping disabled cluster: {}", clusterName);
                    continue;
                }

                try {
                    KafkaTemplate<String, String> template = createKafkaTemplate(config);
                    kafkaTemplates.put(clusterName, template);
                    log.info("✅ Created Kafka template for cluster '{}': {}", clusterName, config.getBootstrapServers());
                } catch (Exception e) {
                    log.warn("⚠️  Could not create Kafka template for cluster '{}': {}. " +
                             "That cluster will be unavailable; other clusters remain operational.",
                             clusterName, e.getMessage());
                }
            }

            log.info("🎉 Kafka Producer Factory Registry ready with {} cluster(s)", kafkaTemplates.size());

        } catch (Exception e) {
            log.warn("⚠️  Kafka Producer Factory Registry initialization encountered an error: {}. " +
                     "Kafka producing will be unavailable.", e.getMessage());
        }
    }
    
    private KafkaTemplate<String, String> createKafkaTemplate(KafkaServerConfig config) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // SSL — truststore is the JKS created by the init container (/app/certs/truststore.jks).
        // Hostname verification is disabled (empty string) because internal k8s service names
        // don't match the wildcard cert; matches the KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=""
        // set on the broker side in kafka.yaml.
        if (config.isSslEnabled()) {
            configProps.put("security.protocol", "SSL");
            configProps.put("ssl.endpoint.identification.algorithm",
                    config.getEndpointIdentificationAlgorithm());
            if (config.getTruststorePath() != null && !config.getTruststorePath().trim().isEmpty()) {
                configProps.put("ssl.truststore.location", config.getTruststorePath());
                configProps.put("ssl.truststore.password",
                        config.getTruststorePassword() != null ? config.getTruststorePassword() : "");
            }
            log.info("🔒 SSL enabled for Kafka cluster '{}' (truststore: {})",
                    config.getName(), config.getTruststorePath());
        }

        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }
    
    // ========== Public API ==========
    
    public KafkaTemplate<String, String> getKafkaTemplate(String clusterName) {
        if (clusterName == null || clusterName.trim().isEmpty()) {
            return getDefaultKafkaTemplate();
        }
        
        KafkaTemplate<String, String> template = kafkaTemplates.get(clusterName);
        if (template == null) {
            throw new IllegalArgumentException(
                "Kafka template not found for cluster '" + clusterName + "'. " +
                "Available clusters: " + kafkaTemplates.keySet()
            );
        }
        return template;
    }
    
    public KafkaTemplate<String, String> getDefaultKafkaTemplate() {
        if (!kafkaTemplates.isEmpty()) {
            return kafkaTemplates.values().iterator().next();
        }
        throw new IllegalStateException(
            "No Kafka template available. " +
            "Configure at least one cluster via kafka.registry.<name>.*"
        );
    }

    public boolean hasKafkaTemplate(String clusterName) {
        return kafkaTemplates.containsKey(clusterName);
    }
    
    public java.util.Set<String> getClusterNames() {
        return kafkaTemplates.keySet();
    }
    
    public boolean isMultiClusterMode() {
        return !kafkaTemplates.isEmpty();
    }
} 
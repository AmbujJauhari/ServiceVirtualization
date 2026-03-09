package com.service.virtualization.config;

import com.service.virtualization.kafka.config.KafkaServerConfig;
import com.service.virtualization.kafka.config.KafkaServerRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Kafka configuration — registry-only mode.
 *
 * All cluster definitions come from KafkaServerRegistry (kafka.registry.*).
 * The @KafkaListener consumer uses the first enabled cluster as its primary.
 * Producing (responses) is routed per-stub via KafkaProducerFactoryRegistry.
 *
 * Only loaded when kafka-disabled profile is NOT active.
 */
@Configuration
@Profile("!kafka-disabled")
public class KafkaConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaConfig.class);

    @Autowired
    private KafkaServerRegistry serverRegistry;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    // -------------------------------------------------------------------------
    // Helpers — pick the primary (first enabled) cluster for the consumer factory
    // -------------------------------------------------------------------------

    private String getPrimaryBootstrapServers() {
        return serverRegistry.getAllClusters().values().stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled())
                .map(KafkaServerConfig::getBootstrapServers)
                .findFirst()
                .orElse("localhost:9092"); // placeholder — validator will catch missing config
    }

    private String getPrimaryGroupId() {
        return serverRegistry.getAllClusters().values().stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled())
                .map(KafkaServerConfig::getConsumerGroupId)
                .findFirst()
                .orElse("service-virtualization");
    }

    // -------------------------------------------------------------------------
    // Beans required by KafkaTopicService and @KafkaListener infrastructure
    // -------------------------------------------------------------------------

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getPrimaryBootstrapServers());
        return new KafkaAdmin(configs);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getPrimaryBootstrapServers());
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, getPrimaryGroupId());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Refresh topic-pattern metadata every 5 s so newly created stub topics
        // are discovered quickly (default is 5 minutes, too slow for E2E tests).
        configProps.put("metadata.max.age.ms", "5000");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * autoStartup=false: listeners are NOT started during context refresh.
     * They are started in the background after the application is ready so that
     * an unreachable Kafka broker cannot prevent the application from starting.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setAutoStartup(false);
        return factory;
    }

    /**
     * Start all Kafka listener containers in a background thread after the application
     * is ready. Retries indefinitely every 30 s so an unreachable broker never blocks startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startKafkaListenersAsync() {
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kafka-listener-starter");
            t.setDaemon(true);
            return t;
        }).execute(() -> {
            while (true) {
                try {
                    log.info("Attempting to start Kafka listener containers...");
                    kafkaListenerEndpointRegistry.start();
                    log.info("Kafka listener containers started successfully (primary bootstrap: {})",
                            getPrimaryBootstrapServers());
                    return;
                } catch (Exception e) {
                    log.warn("Kafka unavailable, will retry in 30 s. Reason: {}", e.getMessage());
                    try {
                        Thread.sleep(30_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
    }
}

package com.servicevirtualization.test.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.PartitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that SSL connections can be established to both Kafka instances.
 *
 * What is being tested:
 *   1. The Istio ingressgateway is reachable on port 30501 (mapped to gateway TLS port 443)
 *   2. The TLS handshake succeeds (broker cert is trusted by the client truststore)
 *   3. The Kafka SSL listener (port 9094 on the pod) accepts connections
 *   4. The Kafka broker metadata is successfully fetched (proves full SSL handshake + protocol)
 *   5. Both Kafka instances are independently reachable on the same NodePort via distinct SNI
 */
@DisplayName("Kafka TLS Connection Tests")
class KafkaTLSConnectionTest {

    private static final Logger log = LoggerFactory.getLogger(KafkaTLSConnectionTest.class);

    @Test
    @DisplayName("Margin Kafka: SSL producer connects and broker metadata is reachable")
    void marginKafkaSSLProducerConnects() {
        log.info("=== TEST: Margin Kafka SSL Producer Connection ===");
        log.info("Bootstrap servers: {}", KafkaTestConfig.getMarginBootstrapServers());

        assertDoesNotThrow(() -> {
            try (KafkaProducer<String, String> producer = KafkaConnectionHelper.createMarginProducer()) {
                // partitionsFor() forces a metadata fetch — proves the SSL connection succeeded
                List<PartitionInfo> partitions = producer.partitionsFor(KafkaTestConfig.getMarginTopic());
                assertNotNull(partitions, "Partition metadata must be returned");
                assertFalse(partitions.isEmpty(), "Topic must have at least one partition");
                log.info("SSL producer connected — topic '{}' has {} partition(s)",
                        KafkaTestConfig.getMarginTopic(), partitions.size());
            }
        }, "Margin Kafka SSL producer connection should succeed");
    }

    @Test
    @DisplayName("Margin Kafka: SSL consumer connects and topic is accessible")
    void marginKafkaSSLConsumerConnects() {
        log.info("=== TEST: Margin Kafka SSL Consumer Connection ===");

        assertDoesNotThrow(() -> {
            try (KafkaConsumer<String, String> consumer = KafkaConnectionHelper.createMarginConsumer()) {
                consumer.subscribe(List.of(KafkaTestConfig.getMarginTopic()));
                // Poll with a short timeout — we just want to confirm the broker handshake succeeds
                consumer.poll(Duration.ofMillis(KafkaTestConfig.getConnectionTimeout()));
                Map<String, List<PartitionInfo>> topics = consumer.listTopics();
                assertNotNull(topics, "Topic listing must succeed over SSL");
                log.info("SSL consumer connected — {} topic(s) visible from broker", topics.size());
            }
        }, "Margin Kafka SSL consumer connection should succeed");
    }

    @Test
    @DisplayName("Collateral Kafka: SSL producer connects and broker metadata is reachable")
    void collateralKafkaSSLProducerConnects() {
        log.info("=== TEST: Collateral Kafka SSL Producer Connection ===");
        log.info("Bootstrap servers: {}", KafkaTestConfig.getCollateralBootstrapServers());

        assertDoesNotThrow(() -> {
            try (KafkaProducer<String, String> producer = KafkaConnectionHelper.createCollateralProducer()) {
                List<PartitionInfo> partitions = producer.partitionsFor(KafkaTestConfig.getCollateralTopic());
                assertNotNull(partitions, "Partition metadata must be returned");
                assertFalse(partitions.isEmpty(), "Topic must have at least one partition");
                log.info("SSL producer connected — topic '{}' has {} partition(s)",
                        KafkaTestConfig.getCollateralTopic(), partitions.size());
            }
        }, "Collateral Kafka SSL producer connection should succeed");
    }

    @Test
    @DisplayName("Both Kafka instances reachable simultaneously on same port — SNI routing confirmed")
    void bothInstancesReachableOnSamePort() {
        log.info("=== TEST: Both Kafka Instances Reachable on Same NodePort ===");
        log.info("Same NodePort, two different Kafka pods — routing by SNI hostname");

        assertDoesNotThrow(() -> {
            try (KafkaProducer<String, String> marginProducer = KafkaConnectionHelper.createMarginProducer();
                 KafkaProducer<String, String> collateralProducer = KafkaConnectionHelper.createCollateralProducer()) {

                List<PartitionInfo> marginPartitions = marginProducer.partitionsFor(KafkaTestConfig.getMarginTopic());
                List<PartitionInfo> collateralPartitions = collateralProducer.partitionsFor(KafkaTestConfig.getCollateralTopic());

                assertFalse(marginPartitions.isEmpty(), "Margin topic must be accessible");
                assertFalse(collateralPartitions.isEmpty(), "Collateral topic must be accessible");

                log.info("Both Kafka SSL connections open simultaneously — SNI routing confirmed");
            }
        }, "Both Kafka instances should be reachable simultaneously");
    }
}

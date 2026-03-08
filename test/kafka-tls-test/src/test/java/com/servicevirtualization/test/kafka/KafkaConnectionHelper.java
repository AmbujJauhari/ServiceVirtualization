package com.servicevirtualization.test.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Creates Kafka producers and consumers over SSL using Istio TLS PASSTHROUGH routing.
 *
 * Connection flow:
 *   Client → SSL → Istio gateway (reads SNI from bootstrap hostname, no decrypt)
 *          → Kafka pod:9094 → pod terminates SSL end-to-end
 *
 * External port design (local K3s):
 *   The Kafka broker advertises: SSL://events.kafka.margin.service-virtualization.local:30501
 *   The bootstrap server AND the reconnect-after-metadata both use port 30501,
 *   which is the NodePort mapping to the Istio gateway's TLS port 443.
 *   externalPort is set to 30501 in kafka-events.yaml to enable this.
 *
 * The Kafka pod presents the wildcard cert (*.service-virtualization.local) from
 * margin-tls-secret, converted to JKS by the cert-converter init container.
 * The client truststore (truststore.jks in certs/) must contain this wildcard cert.
 *
 * Hostname verification is disabled (ssl.endpoint.identification.algorithm="") because
 * the wildcard cert does not cover multi-level subdomains like
 * events.kafka.margin.service-virtualization.local.
 * For production, use a cert with the correct SAN and enable HTTPS verification.
 */
public class KafkaConnectionHelper {

    private static final Logger log = LoggerFactory.getLogger(KafkaConnectionHelper.class);

    /**
     * Builds the common SSL properties used by both producers and consumers.
     */
    private static Properties sslProperties() {
        Properties ssl = new Properties();
        ssl.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        ssl.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, KafkaTestConfig.getTrustStoreLocation());
        ssl.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, KafkaTestConfig.getTrustStorePassword());
        // Empty string disables hostname verification (required for wildcard cert on multi-level subdomain)
        ssl.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                KafkaTestConfig.getEndpointIdentificationAlgorithm());
        return ssl;
    }

    /**
     * Creates a KafkaProducer<String, String> with SSL for the given bootstrap servers.
     *
     * @param bootstrapServers e.g. "events.kafka.margin.service-virtualization.local:30501"
     * @return a producer — caller must close it
     */
    public static KafkaProducer<String, String> createProducer(String bootstrapServers) {
        log.info("Creating Kafka SSL producer: bootstrapServers={}", bootstrapServers);

        Properties props = sslProperties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Ensure all brokers acknowledge the write (single-broker cluster, so this is immediate)
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                String.valueOf(KafkaTestConfig.getProducerTimeout()));

        return new KafkaProducer<>(props);
    }

    /**
     * Creates a KafkaConsumer<String, String> with SSL for the given bootstrap servers.
     * Each consumer uses a unique group ID suffix to ensure it receives messages from
     * the latest offset regardless of prior test runs.
     *
     * @param bootstrapServers e.g. "events.kafka.margin.service-virtualization.local:30501"
     * @param groupId          base consumer group ID (a UUID suffix is appended)
     * @return a consumer — caller must close it
     */
    public static KafkaConsumer<String, String> createConsumer(String bootstrapServers, String groupId) {
        String uniqueGroupId = groupId + "-" + UUID.randomUUID();
        log.info("Creating Kafka SSL consumer: bootstrapServers={} groupId={}", bootstrapServers, uniqueGroupId);

        Properties props = sslProperties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, uniqueGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Read from the beginning of the topic partition for this new group
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                String.valueOf(KafkaTestConfig.getConnectionTimeout()));
        // Disable auto-commit — tests use manual control over what has been consumed
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new KafkaConsumer<>(props);
    }

    /**
     * Convenience: creates a producer for the Margin Events Kafka instance.
     */
    public static KafkaProducer<String, String> createMarginProducer() {
        return createProducer(KafkaTestConfig.getMarginBootstrapServers());
    }

    /**
     * Convenience: creates a consumer for the Margin Events Kafka instance.
     */
    public static KafkaConsumer<String, String> createMarginConsumer() {
        return createConsumer(KafkaTestConfig.getMarginBootstrapServers(),
                KafkaTestConfig.getMarginConsumerGroup());
    }

    /**
     * Convenience: creates a producer for the Collateral Events Kafka instance.
     */
    public static KafkaProducer<String, String> createCollateralProducer() {
        return createProducer(KafkaTestConfig.getCollateralBootstrapServers());
    }

    /**
     * Convenience: creates a consumer for the Collateral Events Kafka instance.
     */
    public static KafkaConsumer<String, String> createCollateralConsumer() {
        return createConsumer(KafkaTestConfig.getCollateralBootstrapServers(),
                KafkaTestConfig.getCollateralConsumerGroup());
    }

    /**
     * Subscribes a consumer to a topic and seeks to the end so that only messages
     * published AFTER this call are visible. Call this before the producer sends.
     */
    public static void subscribeFromEnd(KafkaConsumer<String, String> consumer, String topic) {
        consumer.subscribe(List.of(topic));
        // Poll once to trigger partition assignment, then seek to end
        consumer.poll(java.time.Duration.ofMillis(500));
        consumer.seekToEnd(consumer.assignment());
        // Trigger the seek — seekToEnd is lazy until a position() or poll() call
        consumer.assignment().forEach(consumer::position);
        log.info("Consumer subscribed to topic '{}' and seeked to end", topic);
    }
}

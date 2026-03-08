package com.servicevirtualization.test.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end message flow tests for Kafka: produce a message, consume it back.
 *
 * These tests prove that:
 *   1. SSL connection is established (prerequisite)
 *   2. Topics are writable and readable
 *   3. Messages can be produced and consumed through the Istio TLS passthrough
 *   4. Message content is preserved (no corruption through the Istio proxy)
 *   5. Margin and Collateral Kafka instances are isolated — a message produced
 *      to the margin cluster cannot be consumed from the collateral cluster
 *
 * Note on consumer strategy:
 *   KafkaConnectionHelper.subscribeFromEnd() is called BEFORE the producer sends,
 *   which seeks the consumer to the current end of the partition. This prevents
 *   consuming stale messages from previous test runs and ensures only the message
 *   produced within this test is received.
 */
@DisplayName("Kafka Message Flow Tests")
class KafkaMessageFlowTest {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageFlowTest.class);

    @Test
    @DisplayName("Margin Kafka: produce and consume message via SSL")
    void marginKafkaProduceAndConsumeMessage() throws Exception {
        log.info("=== TEST: Margin Kafka SSL produce/consume ===");

        String messageKey = "margin-test-" + UUID.randomUUID();
        String messageValue = "{ \"cluster\": \"margin-events\", \"key\": \"" + messageKey + "\", \"test\": true }";
        String topic = KafkaTestConfig.getMarginTopic();

        try (KafkaProducer<String, String> producer = KafkaConnectionHelper.createMarginProducer();
             KafkaConsumer<String, String> consumer = KafkaConnectionHelper.createMarginConsumer()) {

            // Subscribe and seek to end BEFORE producing so we only see our new message
            KafkaConnectionHelper.subscribeFromEnd(consumer, topic);

            // PRODUCE
            Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, messageKey, messageValue));
            RecordMetadata meta = future.get(KafkaTestConfig.getProducerTimeout(), TimeUnit.MILLISECONDS);
            producer.flush();
            log.info("PRODUCED message to margin '{}': partition={} offset={}",
                    topic, meta.partition(), meta.offset());

            // CONSUME — poll until we find our message or timeout
            String received = pollForMessage(consumer, messageKey, KafkaTestConfig.getPollTimeout());

            assertNotNull(received, "Message should be received from margin Kafka topic within timeout");
            assertEquals(messageValue, received, "Received message value must match produced value");
            log.info("CONSUMED message from margin Kafka: content matches — SSL flow confirmed");
        }
    }

    @Test
    @DisplayName("Collateral Kafka: produce and consume message via SSL")
    void collateralKafkaProduceAndConsumeMessage() throws Exception {
        log.info("=== TEST: Collateral Kafka SSL produce/consume ===");

        String messageKey = "collateral-test-" + UUID.randomUUID();
        String messageValue = "{ \"cluster\": \"collateral-events\", \"key\": \"" + messageKey + "\", \"test\": true }";
        String topic = KafkaTestConfig.getCollateralTopic();

        try (KafkaProducer<String, String> producer = KafkaConnectionHelper.createCollateralProducer();
             KafkaConsumer<String, String> consumer = KafkaConnectionHelper.createCollateralConsumer()) {

            KafkaConnectionHelper.subscribeFromEnd(consumer, topic);

            Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, messageKey, messageValue));
            RecordMetadata meta = future.get(KafkaTestConfig.getProducerTimeout(), TimeUnit.MILLISECONDS);
            producer.flush();
            log.info("PRODUCED message to collateral '{}': partition={} offset={}",
                    topic, meta.partition(), meta.offset());

            String received = pollForMessage(consumer, messageKey, KafkaTestConfig.getPollTimeout());

            assertNotNull(received, "Message should be received from collateral Kafka topic within timeout");
            assertEquals(messageValue, received, "Received message value must match produced value");
            log.info("CONSUMED message from collateral Kafka: content matches — SSL flow confirmed");
        }
    }

    @Test
    @DisplayName("Cluster isolation: margin message not visible on collateral cluster")
    void marginMessageNotVisibleOnCollateralCluster() throws Exception {
        log.info("=== TEST: Cluster isolation — margin messages stay in margin ===");

        String messageKey = "isolation-test-" + UUID.randomUUID();
        String messageValue = "{ \"cluster\": \"margin-events\", \"key\": \"" + messageKey + "\" }";
        String marginTopic = KafkaTestConfig.getMarginTopic();
        String collateralTopic = KafkaTestConfig.getCollateralTopic();

        // Step 1: Subscribe collateral consumer BEFORE producing to margin (seek to end of collateral)
        try (KafkaConsumer<String, String> collateralConsumer = KafkaConnectionHelper.createCollateralConsumer()) {
            KafkaConnectionHelper.subscribeFromEnd(collateralConsumer, collateralTopic);

            // Step 2: Produce to margin cluster
            try (KafkaProducer<String, String> marginProducer = KafkaConnectionHelper.createMarginProducer()) {
                Future<RecordMetadata> future = marginProducer.send(
                        new ProducerRecord<>(marginTopic, messageKey, messageValue));
                future.get(KafkaTestConfig.getProducerTimeout(), TimeUnit.MILLISECONDS);
                marginProducer.flush();
                log.info("PRODUCED message to margin cluster with key={}", messageKey);
            }

            // Step 3: Verify it does NOT appear on the collateral cluster
            String collateralReceived = pollForMessage(collateralConsumer, messageKey, 3000);
            assertNull(collateralReceived,
                    "Message produced to margin cluster must NOT be visible on collateral cluster. " +
                    "If this fails, the two Kafka instances are not properly isolated via SNI routing.");

            log.info("Isolation confirmed: margin message not visible on collateral Kafka cluster");
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Polls the consumer until a record with the given key is found or the
     * timeout elapses, returning its value. Returns null if not found.
     */
    private String pollForMessage(KafkaConsumer<String, String> consumer,
                                  String expectedKey,
                                  long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        List<String> received = new ArrayList<>();

        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(
                    Duration.ofMillis(Math.min(1000, deadline - System.currentTimeMillis())));

            for (ConsumerRecord<String, String> record : records) {
                log.debug("POLLED record: key={} partition={} offset={}", record.key(), record.partition(), record.offset());
                if (expectedKey.equals(record.key())) {
                    return record.value();
                }
                received.add(record.key());
            }
        }

        log.warn("Message with key '{}' not found within timeout. Other keys received: {}", expectedKey, received);
        return null;
    }
}

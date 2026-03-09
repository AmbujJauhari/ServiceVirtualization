package com.service.virtualization.jmshelper.service;

import com.service.virtualization.jmshelper.model.ConsumeResponse;
import com.service.virtualization.jmshelper.model.KafkaConsumeRequest;
import com.service.virtualization.jmshelper.model.KafkaPublishRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Publishes and consumes Kafka messages for E2E test purposes.
 *
 * Each consume call creates a fresh consumer with a unique group ID and seeks
 * to the beginning of all partitions. This is safe because E2E tests use unique
 * topic names per test run, ensuring only the expected message(s) are present.
 */
@Service
public class KafkaHelperService {

    private static final Logger log = LoggerFactory.getLogger(KafkaHelperService.class);

    @Value("${jms-helper.kafka.bootstrap-servers:localhost:9092}")
    private String defaultBootstrapServers;

    public void publish(KafkaPublishRequest req) throws ExecutionException, InterruptedException {
        String servers = req.bootstrapServers() != null && !req.bootstrapServers().isBlank()
                ? req.bootstrapServers() : defaultBootstrapServers;

        log.info("Kafka publish → topic={} key={} servers={}", req.topic(), req.key(), servers);

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("request.timeout.ms", "10000");
        props.put("delivery.timeout.ms", "15000");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(req.topic(), req.key(), req.message());
            producer.send(record).get();
            log.info("Kafka message published to {}", req.topic());
        }
    }

    public ConsumeResponse consume(KafkaConsumeRequest req) {
        String servers = req.bootstrapServers() != null && !req.bootstrapServers().isBlank()
                ? req.bootstrapServers() : defaultBootstrapServers;
        String groupId = "sv-e2e-helper-" + UUID.randomUUID();

        log.info("Kafka consume ← topic={} timeout={}ms servers={}", req.topic(), req.effectiveTimeout(), servers);

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("group.id", groupId);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        props.put("session.timeout.ms", "10000");
        props.put("request.timeout.ms", "11000");
        props.put("default.api.timeout.ms", "12000");
        // Refresh metadata every 5 s so newly created topics are discovered quickly
        props.put("metadata.max.age.ms", "5000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(req.topic()));

            Set<TopicPartition> seekedPartitions = new HashSet<>();
            long deadline = System.currentTimeMillis() + req.effectiveTimeout();

            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Math.min(remaining, 500)));

                // Seek any newly assigned partitions to the beginning so we
                // read messages published before this consumer started.
                Set<TopicPartition> assigned = consumer.assignment();
                Set<TopicPartition> newPartitions = new HashSet<>(assigned);
                newPartitions.removeAll(seekedPartitions);
                if (!newPartitions.isEmpty()) {
                    consumer.seekToBeginning(newPartitions);
                    seekedPartitions.addAll(newPartitions);
                    log.info("Sought to beginning for {} partitions on {}", newPartitions.size(), req.topic());
                    // Re-poll immediately after seek
                    continue;
                }

                for (ConsumerRecord<String, String> record : records) {
                    log.info("Kafka message received on {} key={}", req.topic(), record.key());
                    return new ConsumeResponse(true, record.value(), record.key(), null);
                }
            }

            log.info("Kafka consume timed out — no message on {}", req.topic());
            return ConsumeResponse.notFound();
        } catch (Exception e) {
            log.error("Kafka consume error: {}", e.getMessage(), e);
            return ConsumeResponse.notFound();
        }
    }
}

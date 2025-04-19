package com.service.virtualization.service.impl;

import com.service.virtualization.service.KafkaMessageService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of the Kafka message service
 */
@Service
public class KafkaMessageServiceImpl implements KafkaMessageService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageServiceImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAdmin kafkaAdmin;
    
    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Autowired
    public KafkaMessageServiceImpl(KafkaTemplate<String, String> kafkaTemplate, KafkaAdmin kafkaAdmin) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public void publishMessage(String topic, String key, String content, Map<String, String> headers) {
        logger.info("Publishing message to topic: {}", topic);
        
        // Create record with headers if provided
        ProducerRecord<String, String> record;
        
        if (headers != null && !headers.isEmpty()) {
            List<Header> kafkaHeaders = headers.entrySet().stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());
                
            record = new ProducerRecord<>(topic, null, key, content, kafkaHeaders);
        } else {
            record = new ProducerRecord<>(topic, key, content);
        }
        
        // Send the message
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        
        future.addCallback(
            result -> logger.info("Message sent successfully to topic: {}", topic),
            ex -> logger.error("Failed to send message to topic: {}", topic, ex)
        );
    }

    @Override
    public List<String> getAvailableTopics() {
        logger.info("Getting available Kafka topics");
        
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult topics = adminClient.listTopics();
            return new ArrayList<>(topics.names().get());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to get Kafka topics", e);
            Thread.currentThread().interrupt();
            return List.of(); // Return empty list on error
        }
    }
} 
package com.service.virtualization.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing Kafka topics
 */
@Service
public class KafkaTopicService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicService.class);

    private final KafkaAdmin kafkaAdmin;

    @Autowired
    public KafkaTopicService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    /**
     * Check if a topic exists
     *
     * @param topicName the name of the topic to check
     * @return true if the topic exists, false otherwise
     */
    public boolean topicExists(String topicName) {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            Set<String> topicNames = listTopicsResult.names().get();
            return topicNames.contains(topicName);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to check if topic '{}' exists", topicName, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Create a topic if it doesn't exist
     *
     * @param topicName the name of the topic to create
     * @param partitions the number of partitions for the topic
     * @param replicationFactor the replication factor for the topic
     * @throws RuntimeException if topic creation fails
     */
    public void createTopicIfNotExists(String topicName, int partitions, short replicationFactor) {
        if (topicExists(topicName)) {
            logger.debug("Topic '{}' already exists, skipping creation", topicName);
            return;
        }

        logger.info("Creating topic '{}' with {} partitions and replication factor {}", 
                   topicName, partitions, replicationFactor);

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singletonList(newTopic));
            
            // Wait for the topic to be created
            createTopicsResult.all().get();
            logger.info("Successfully created topic '{}'", topicName);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to create topic '{}'", topicName, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to create topic: " + topicName, e);
        }
    }

    /**
     * Create a topic with default settings (1 partition, replication factor 1)
     *
     * @param topicName the name of the topic to create
     */
    public void createTopicIfNotExists(String topicName) {
        createTopicIfNotExists(topicName, 1, (short) 1);
    }

    /**
     * Ensure multiple topics exist, creating them if necessary
     *
     * @param topicNames the names of the topics to ensure exist
     */
    public void ensureTopicsExist(String... topicNames) {
        for (String topicName : topicNames) {
            if (topicName != null && !topicName.trim().isEmpty()) {
                createTopicIfNotExists(topicName.trim());
            }
        }
    }
} 
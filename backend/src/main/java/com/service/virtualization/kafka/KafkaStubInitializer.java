package com.service.virtualization.kafka;

import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.service.KafkaTopicService;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes all active Kafka stubs from the database when the application starts
 * Ensures that all required topics exist for active stubs
 */
@Component
@Profile("!kafka-disabled")
public class KafkaStubInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaStubInitializer.class);
    
    private final KafkaStubService kafkaStubService;
    private final KafkaTopicService kafkaTopicService;
    
    public KafkaStubInitializer(KafkaStubService kafkaStubService, KafkaTopicService kafkaTopicService) {
        this.kafkaStubService = kafkaStubService;
        this.kafkaTopicService = kafkaTopicService;
    }
    
    @Override
    @Transactional(readOnly = true)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("🚀 Initializing Kafka stubs and topics from database...");
        
        try {
            // Get all active stubs from database
            List<KafkaStub> activeStubs = kafkaStubService.getAllStubs().stream()
                .filter(stub -> stub.status() == StubStatus.ACTIVE)
                .toList();
            
            logger.info("Found {} active Kafka stubs to initialize", activeStubs.size());
            
            if (activeStubs.isEmpty()) {
                logger.info("No active Kafka stubs found, skipping topic initialization");
                return;
            }
            
            // Collect all required topics
            Set<String> requiredTopics = new HashSet<>();
            for (KafkaStub stub : activeStubs) {
                if (stub.requestTopic() != null && !stub.requestTopic().trim().isEmpty()) {
                    requiredTopics.add(stub.requestTopic().trim());
                }
                
                if (stub.responseTopic() != null && !stub.responseTopic().trim().isEmpty()) {
                    requiredTopics.add(stub.responseTopic().trim());
                } else if (stub.requestTopic() != null && !stub.requestTopic().trim().isEmpty()) {
                    // Auto-generate response topic if not specified
                    requiredTopics.add(stub.requestTopic().trim() + "-response");
                }
            }
            
            logger.info("Ensuring {} unique topics exist for active stubs", requiredTopics.size());
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // Create all required topics
            requiredTopics.parallelStream().forEach(topicName -> {
                try {
                    kafkaTopicService.createTopicIfNotExists(topicName);
                    logger.debug("Ensured topic exists: {}", topicName);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("Failed to ensure topic '{}' exists: {}", topicName, e.getMessage());
                    errorCount.incrementAndGet();
                }
            });
            
            logger.info("🔧 Kafka initialization complete. Topics ensured: {}, Failed: {}", 
                       successCount.get(), errorCount.get());
            
            if (errorCount.get() > 0) {
                logger.warn("⚠️ Some topics failed to initialize. Kafka stubs may not work properly until topics are created.");
                logger.warn("💡 The auto-recovery service will continue attempting to recreate missing topics every 30 seconds.");
            } else {
                logger.info("✅ All Kafka stub topics are ready!");
            }
            
        } catch (Exception e) {
            logger.error("💥 Critical error initializing Kafka stubs and topics", e);
            logger.warn("🔄 Auto-recovery service will handle topic creation as needed");
            // Don't rethrow - we want the application to continue starting up
        }
    }
} 
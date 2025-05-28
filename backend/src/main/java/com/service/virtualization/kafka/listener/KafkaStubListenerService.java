package com.service.virtualization.kafka.listener;

import com.service.virtualization.kafka.repository.KafkaStubRepository;
import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.service.KafkaMessageService;
import com.service.virtualization.kafka.service.KafkaCallbackService;
import com.service.virtualization.kafka.service.KafkaTopicService;
import com.service.virtualization.model.StubStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Service to listen to Kafka topics and process messages based on stubs
 */
@Service
public class KafkaStubListenerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStubListenerService.class);
    
    private final KafkaStubRepository kafkaStubRepository;
    private final KafkaMessageService kafkaMessageService;
    private final KafkaCallbackService kafkaCallbackService;
    private final KafkaTopicService kafkaTopicService;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler;
    
    @Autowired
    public KafkaStubListenerService(
            KafkaStubRepository kafkaStubRepository,
            KafkaMessageService kafkaMessageService,
            KafkaCallbackService kafkaCallbackService,
            KafkaTopicService kafkaTopicService) {
        this.kafkaStubRepository = kafkaStubRepository;
        this.kafkaMessageService = kafkaMessageService;
        this.kafkaCallbackService = kafkaCallbackService;
        this.kafkaTopicService = kafkaTopicService;
        this.restTemplate = new RestTemplate();
        this.scheduler = Executors.newScheduledThreadPool(5);
        
        // Start auto-recovery check
        startPeriodicTopicRecovery();
    }
    
    /**
     * Kafka listener method
     * Uses a pattern to match multiple topics
     */
    @KafkaListener(topicPattern = ".*", groupId = "${kafka.consumer.group-id:service-virtualization}")
    public void onMessage(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            ConsumerRecord<String, String> record) {
        
        logger.info("Received message on topic: {}, with key: {}", topic, key);
        
        // Find active stubs for this request topic
        List<KafkaStub> stubs = kafkaStubRepository.findActiveStubsByRequestTopic(topic);
        
        if (stubs.isEmpty()) {
            logger.info("No active stubs found for topic: {}", topic);
            return;
        }
        
        // Process each matching stub
        for (KafkaStub stub : stubs) {
            processStub(stub, message, key, topic, record);
        }
    }
    
    /**
     * Process stub based on message content
     */
    private void processStub(KafkaStub stub, String message, String key, String topic, ConsumerRecord<String, String> record) {
        // Skip if key pattern doesn't match
        if (stub.keyPattern() != null && !stub.keyPattern().isEmpty() && key != null) {
            if (!Pattern.matches(stub.keyPattern(), key)) {
                logger.debug("Key pattern didn't match for stub: {}", stub.name());
                return;
            }
        }
        
        // Skip if value pattern doesn't match
        if (stub.valuePattern() != null && !stub.valuePattern().isEmpty()) {
            if (!Pattern.matches(stub.valuePattern(), message)) {
                logger.debug("Value pattern didn't match for stub: {}", stub.name());
                return;
            }
        }
        
        logger.info("Stub matched: {}", stub.name());
        
        // Handle response according to stub configuration
        if ("direct".equals(stub.responseType())) {
            // Add latency if specified
            if (stub.latency() != null && stub.latency() > 0) {
                delayedResponse(stub, topic);
            } else {
                sendResponse(stub, topic);
            }
        } else if ("callback".equals(stub.responseType())) {
            // Execute HTTP callback which will return response data and publish to Kafka
            if (stub.latency() != null && stub.latency() > 0) {
                // Delayed callback
                scheduler.schedule(() -> {
                    kafkaCallbackService.executeCallbackAsync(stub, topic, key, message);
                }, stub.latency(), TimeUnit.MILLISECONDS);
            } else {
                // Immediate callback
                kafkaCallbackService.executeCallbackAsync(stub, topic, key, message);
            }
            logger.info("Callback execution initiated for stub: {}", stub.name());
        }
    }
    
    /**
     * Send delayed response
     */
    private void delayedResponse(KafkaStub stub, String topic) {
        scheduler.schedule(() -> {
            sendResponse(stub, topic);
        }, stub.latency(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Send response message
     */
    private void sendResponse(KafkaStub stub, String requestTopic) {
        String responseTopic = stub.responseTopic();
        
        // If no response topic is configured, use request topic with "-response" suffix
        if (responseTopic == null || responseTopic.trim().isEmpty()) {
            responseTopic = requestTopic + "-response";
        }
        
        // If response topic is the same as request topic, append "-response" to avoid loops
        if (responseTopic.equals(requestTopic)) {
            responseTopic = requestTopic + "-response";
        }
        
        // Create empty headers map (could be populated from stub configuration)
        Map<String, String> headers = new HashMap<>();
        
        // Publish the response message with responseKey from stub
        kafkaMessageService.publishMessage(
            responseTopic,
            stub.responseKey(), // Use responseKey from stub instead of null
            stub.responseContent(),
            headers
        );
        
        logger.info("Response sent to topic: {} with key: {}", responseTopic, stub.responseKey());
    }
    
    /**
     * Start periodic topic recovery to automatically recreate missing topics
     */
    private void startPeriodicTopicRecovery() {
        // Check every 30 seconds for missing topics and recreate them
        scheduler.scheduleAtFixedRate(this::performTopicRecovery, 30, 30, TimeUnit.SECONDS);
        logger.info("🚀 Started automatic topic recovery service (checks every 30 seconds)");
    }
    
    /**
     * Perform automatic topic recovery for all active stubs
     */
    private void performTopicRecovery() {
        try {
            logger.debug("🔍 Performing automatic topic recovery check...");
            
            List<KafkaStub> activeStubs = kafkaStubRepository.findAll().stream()
                .filter(stub -> stub.status() == StubStatus.ACTIVE)
                .toList();
            
            if (activeStubs.isEmpty()) {
                logger.debug("No active stubs found, skipping topic recovery");
                return;
            }
            
            Set<String> requiredTopics = new HashSet<>();
            
            // Collect all required topics from active stubs
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
            
            // Check and create missing topics
            int createdCount = 0;
            int errorCount = 0;
            for (String topicName : requiredTopics) {
                try {
                    if (!kafkaTopicService.topicExists(topicName)) {
                        kafkaTopicService.createTopicIfNotExists(topicName);
                        createdCount++;
                        logger.warn("🔧 AUTO-RECOVERY: Recreated missing topic '{}' (Kafka may have restarted)", topicName);
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("❌ AUTO-RECOVERY: Failed to recreate topic '{}': {}", topicName, e.getMessage());
                }
            }
            
            if (createdCount > 0) {
                logger.warn("🔧 AUTO-RECOVERY COMPLETED: {} topics recreated for {} active stubs. " +
                           "This suggests Kafka broker was restarted.", createdCount, activeStubs.size());
            }
            
            if (errorCount > 0) {
                logger.error("❌ AUTO-RECOVERY ERRORS: {} topics failed to recreate. Check Kafka broker status.", errorCount);
            }
            
        } catch (Exception e) {
            logger.error("💥 Error during automatic topic recovery: {}", e.getMessage(), e);
        }
    }
} 
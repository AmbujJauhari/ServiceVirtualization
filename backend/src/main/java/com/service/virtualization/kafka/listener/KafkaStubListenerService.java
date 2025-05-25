package com.service.virtualization.kafka.listener;

import com.service.virtualization.kafka.repository.KafkaStubRepository;
import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.service.KafkaMessageService;
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
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler;
    
    @Autowired
    public KafkaStubListenerService(
            KafkaStubRepository kafkaStubRepository,
            KafkaMessageService kafkaMessageService) {
        this.kafkaStubRepository = kafkaStubRepository;
        this.kafkaMessageService = kafkaMessageService;
        this.restTemplate = new RestTemplate();
        this.scheduler = Executors.newScheduledThreadPool(5);
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
        
        // Find active consumer stubs for this topic
        List<KafkaStub> stubs = kafkaStubRepository.findActiveConsumerStubsByTopic(topic);
        
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
        if (stub.getKeyPattern() != null && !stub.getKeyPattern().isEmpty() && key != null) {
            if (!Pattern.matches(stub.getKeyPattern(), key)) {
                logger.debug("Key pattern didn't match for stub: {}", stub.getName());
                return;
            }
        }
        
        // Skip if value pattern doesn't match
        if (stub.getValuePattern() != null && !stub.getValuePattern().isEmpty()) {
            if (!Pattern.matches(stub.getValuePattern(), message)) {
                logger.debug("Value pattern didn't match for stub: {}", stub.getName());
                return;
            }
        }
        
        logger.info("Stub matched: {}", stub.getName());
        
        // Handle response according to stub configuration
        if ("direct".equals(stub.getResponseType())) {
            // Add latency if specified
            if (stub.getLatency() != null && stub.getLatency() > 0) {
                delayedResponse(stub, topic);
            } else {
                sendResponse(stub, topic);
            }
        } else if ("callback".equals(stub.getResponseType())) {
            // For callback type, we would add callback URL handling logic
            // This part can be implemented based on the application needs
            logger.info("Callback response type not implemented yet");
        }
    }
    
    /**
     * Send delayed response
     */
    private void delayedResponse(KafkaStub stub, String topic) {
        scheduler.schedule(() -> {
            sendResponse(stub, topic);
        }, stub.getLatency(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Send response message
     */
    private void sendResponse(KafkaStub stub, String requestTopic) {
        String responseTopic = stub.getTopic(); // Use the stub's configured topic
        
        // If it's the same as request topic, append "-response" to avoid loops
        if (responseTopic.equals(requestTopic)) {
            responseTopic = requestTopic + "-response";
        }
        
        // Create empty headers map (could be populated from stub configuration)
        Map<String, String> headers = new HashMap<>();
        
        // Publish the response message
        kafkaMessageService.publishMessage(
            responseTopic,
            null, // No key for response
            stub.getResponseContent(),
            headers
        );
        
        logger.info("Response sent to topic: {}", responseTopic);
    }
} 
package com.service.virtualization.kafka.listener;

import com.service.virtualization.kafka.repository.KafkaStubRepository;
import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.service.KafkaMessageService;
import com.service.virtualization.kafka.service.KafkaCallbackService;
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
    private final KafkaCallbackService kafkaCallbackService;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler;
    
    @Autowired
    public KafkaStubListenerService(
            KafkaStubRepository kafkaStubRepository,
            KafkaMessageService kafkaMessageService,
            KafkaCallbackService kafkaCallbackService) {
        this.kafkaStubRepository = kafkaStubRepository;
        this.kafkaMessageService = kafkaMessageService;
        this.kafkaCallbackService = kafkaCallbackService;
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
            // Execute HTTP callback which will return response data and publish to Kafka
            if (stub.getLatency() != null && stub.getLatency() > 0) {
                // Delayed callback
                scheduler.schedule(() -> {
                    kafkaCallbackService.executeCallbackAsync(stub, topic, key, message);
                }, stub.getLatency(), TimeUnit.MILLISECONDS);
            } else {
                // Immediate callback
                kafkaCallbackService.executeCallbackAsync(stub, topic, key, message);
            }
            logger.info("Callback execution initiated for stub: {}", stub.getName());
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
        String responseTopic = stub.getResponseTopic();
        
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
            stub.getResponseKey(), // Use responseKey from stub instead of null
            stub.getResponseContent(),
            headers
        );
        
        logger.info("Response sent to topic: {} with key: {}", responseTopic, stub.getResponseKey());
    }
} 
package com.service.virtualization.kafka.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.kafka.model.KafkaStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for handling HTTP callbacks that return dynamic response data for Kafka publishing.
 * 
 * Flow:
 * 1. Send HTTP request to webhook with original request data
 * 2. Webhook returns response with {key, responseContent, responseFormat}
 * 3. Publish the webhook response data to Kafka response topic
 */
@Service
public class KafkaCallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaCallbackService.class);
    
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    private final KafkaMessageService kafkaMessageService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public KafkaCallbackService(KafkaMessageService kafkaMessageService) {
        this.restTemplate = new RestTemplate();
        this.executorService = Executors.newFixedThreadPool(10);
        this.kafkaMessageService = kafkaMessageService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Execute callback: call webhook and publish response to Kafka
     * 
     * @param stub The Kafka stub configuration
     * @param requestTopic The original request topic
     * @param requestKey The original request key
     * @param requestMessage The original request message
     */
    public void executeCallback(KafkaStub stub, String requestTopic, String requestKey, String requestMessage) {
        String callbackUrl = stub.getCallbackUrl();
        
        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            logger.warn("No callback URL configured for stub: {}", stub.getName());
            return;
        }
        
        try {
            // Step 1: Call webhook and get response data
            CallbackResponse callbackResponse = callWebhook(stub, requestTopic, requestKey, requestMessage, callbackUrl);
            
            if (callbackResponse != null) {
                // Step 2: Publish the webhook response to Kafka
                publishWebhookResponseToKafka(stub, requestTopic, callbackResponse);
            }
            
        } catch (Exception e) {
            logger.error("💥 Error executing callback for stub '{}': {}", stub.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Execute callback asynchronously
     */
    public void executeCallbackAsync(KafkaStub stub, String requestTopic, String requestKey, String requestMessage) {
        CompletableFuture.runAsync(() -> {
            executeCallback(stub, requestTopic, requestKey, requestMessage);
        }, executorService);
    }
    
    /**
     * Call the webhook and parse response
     */
    private CallbackResponse callWebhook(KafkaStub stub, String requestTopic, String requestKey, String requestMessage, String callbackUrl) {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Add custom headers from stub configuration
            if (stub.getCallbackHeaders() != null) {
                stub.getCallbackHeaders().forEach((key, value) -> {
                    headers.add(key, value);
                });
            }
            
            // Add metadata headers
            headers.add("X-Kafka-Request-Topic", requestTopic);
            headers.add("X-Kafka-Request-Key", requestKey != null ? requestKey : "");
            headers.add("X-Stub-Name", stub.getName());
            headers.add("X-Stub-Id", stub.getId());
            
            // Prepare request payload for webhook
            Map<String, Object> requestPayload = createWebhookRequestPayload(stub, requestTopic, requestKey, requestMessage);
            
            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);
            
            logger.info("🔗 Calling webhook for stub '{}' at URL: {}", stub.getName(), callbackUrl);
            logger.debug("📤 Webhook request payload: {}", requestPayload);
            
            // Call webhook
            ResponseEntity<String> response = restTemplate.exchange(
                callbackUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            logger.info("✅ Webhook call successful for stub '{}'. Status: {}", 
                       stub.getName(), response.getStatusCode());
            logger.debug("📨 Webhook response: {}", response.getBody());
            
            // Parse webhook response
            return parseWebhookResponse(response.getBody());
            
        } catch (RestClientException e) {
            logger.error("❌ Webhook call failed for stub '{}' to URL: {}. Error: {}", 
                        stub.getName(), callbackUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * Create the request payload to send to webhook
     */
    private Map<String, Object> createWebhookRequestPayload(KafkaStub stub, String requestTopic, String requestKey, String requestMessage) {
        Map<String, Object> payload = new HashMap<>();
        
        // Stub information
        payload.put("stubId", stub.getId());
        payload.put("stubName", stub.getName());
        
        // Request information
        Map<String, Object> request = new HashMap<>();
        request.put("topic", requestTopic);
        request.put("key", requestKey);
        request.put("message", requestMessage);
        payload.put("request", request);
        
        // Timestamp
        payload.put("timestamp", System.currentTimeMillis());
        
        return payload;
    }
    
    /**
     * Parse webhook response to extract response data
     * Expected format: {"key": "...", "responseContent": "...", "responseFormat": "...", "responseTopic": "..."}
     */
    private CallbackResponse parseWebhookResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.warn("Empty response from webhook");
                return null;
            }
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String key = jsonNode.has("key") ? jsonNode.get("key").asText() : null;
            String responseContent = jsonNode.has("responseContent") ? jsonNode.get("responseContent").asText() : null;
            String responseFormat = jsonNode.has("responseFormat") ? jsonNode.get("responseFormat").asText() : "JSON";
            String responseTopic = jsonNode.has("responseTopic") ? jsonNode.get("responseTopic").asText() : null;
            
            if (responseContent == null || responseContent.trim().isEmpty()) {
                logger.warn("No responseContent found in webhook response");
                return null;
            }
            
            return new CallbackResponse(key, responseContent, responseFormat, responseTopic);
            
        } catch (Exception e) {
            logger.error("Error parsing webhook response: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Publish the webhook response data to Kafka
     */
    private void publishWebhookResponseToKafka(KafkaStub stub, String requestTopic, CallbackResponse callbackResponse) {
        try {
            // Determine response topic with priority order:
            // 1. Webhook response responseTopic (highest priority)
            // 2. Stub configuration responseTopic
            // 3. Auto-generated from request topic (fallback)
            String responseTopic = callbackResponse.getResponseTopic();
            if (responseTopic == null || responseTopic.trim().isEmpty()) {
                responseTopic = stub.getResponseTopic();
                if (responseTopic == null || responseTopic.trim().isEmpty()) {
                    responseTopic = requestTopic + "-response";
                }
            }
            
            // If response topic is the same as request topic, append "-response" to avoid loops
            if (responseTopic.equals(requestTopic)) {
                responseTopic = requestTopic + "-response";
                logger.warn("Response topic was same as request topic, changed to: {}", responseTopic);
            }
            
            // Use key from webhook response, or auto-generate if not provided
            String responseKey = callbackResponse.getKey();
            if (responseKey == null || responseKey.trim().isEmpty()) {
                responseKey = UUID.randomUUID().toString();
                logger.debug("Auto-generated response key for webhook response: {}", responseKey);
            }
            
            // Prepare headers with format information
            Map<String, String> kafkaHeaders = new HashMap<>();
            kafkaHeaders.put("content-format", callbackResponse.getResponseFormat());
            kafkaHeaders.put("source-stub-id", stub.getId());
            kafkaHeaders.put("source-stub-name", stub.getName());
            kafkaHeaders.put("response-type", "callback");
            kafkaHeaders.put("response-source", "webhook");
            
            // Add responseTopic source information
            if (callbackResponse.getResponseTopic() != null && !callbackResponse.getResponseTopic().trim().isEmpty()) {
                kafkaHeaders.put("response-topic-source", "webhook");
            } else if (stub.getResponseTopic() != null && !stub.getResponseTopic().trim().isEmpty()) {
                kafkaHeaders.put("response-topic-source", "stub");
            } else {
                kafkaHeaders.put("response-topic-source", "auto-generated");
            }
            
            // Publish the webhook response to Kafka
            kafkaMessageService.publishMessage(
                responseTopic,
                responseKey,
                callbackResponse.getResponseContent(),
                kafkaHeaders
            );
            
            logger.info("📨 Webhook response published to Kafka - Topic: {}, Key: {}, Format: {}", 
                       responseTopic, responseKey, callbackResponse.getResponseFormat());
            
        } catch (Exception e) {
            logger.error("💥 Error publishing webhook response to Kafka: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Execute callback with retry mechanism
     */
    public void executeCallbackWithRetry(KafkaStub stub, String requestTopic, String requestKey, String requestMessage, int maxRetries) {
        CompletableFuture.runAsync(() -> {
            int attempt = 0;
            boolean success = false;
            
            while (attempt <= maxRetries && !success) {
                try {
                    executeCallback(stub, requestTopic, requestKey, requestMessage);
                    success = true;
                    logger.info("✅ Callback with retry successful for stub '{}' on attempt {}", stub.getName(), attempt + 1);
                } catch (Exception e) {
                    attempt++;
                    if (attempt <= maxRetries) {
                        logger.warn("🔄 Callback attempt {} failed for stub '{}', retrying... Error: {}", 
                                   attempt, stub.getName(), e.getMessage());
                        try {
                            Thread.sleep(1000 * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        logger.error("💀 All callback attempts failed for stub '{}' after {} tries. Last error: {}", 
                                    stub.getName(), maxRetries, e.getMessage());
                    }
                }
            }
        }, executorService);
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * DTO for callback response data
     */
    public static class CallbackResponse {
        private final String key;
        private final String responseContent;
        private final String responseFormat;
        private final String responseTopic;
        
        public CallbackResponse(String key, String responseContent, String responseFormat, String responseTopic) {
            this.key = key;
            this.responseContent = responseContent;
            this.responseFormat = responseFormat;
            this.responseTopic = responseTopic;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getResponseContent() {
            return responseContent;
        }
        
        public String getResponseFormat() {
            return responseFormat;
        }
        
        public String getResponseTopic() {
            return responseTopic;
        }
    }
} 
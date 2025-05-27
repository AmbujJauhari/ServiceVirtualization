package com.service.virtualization.kafka.controller;

import com.service.virtualization.kafka.service.KafkaCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller demonstrating webhook response format for Kafka callbacks
 */
@RestController
@RequestMapping("/webhook/test")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"}, allowCredentials = "true")
public class KafkaWebhookTestController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaWebhookTestController.class);

    /**
     * Example webhook that processes order requests and returns response data
     */
    @PostMapping("/process-order")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> processOrderWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.info("📥 Received webhook call for order processing: {}", requestPayload);
        
        // Extract request information
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestMessage = (String) request.get("message");
        String requestTopic = (String) request.get("topic");
        
        // Create response using CallbackResponse with specific response topic
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            "order-response-" + System.currentTimeMillis(),
            createOrderResponse(requestMessage),
            "JSON",
            requestTopic + "-orders-processed" // Custom response topic
        );
        
        logger.info("📤 Returning webhook response: key={}, format={}, topic={}", 
                   response.getKey(), response.getResponseFormat(), response.getResponseTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * Example webhook that processes payment requests
     */
    @PostMapping("/process-payment")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> processPaymentWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.info("📥 Received webhook call for payment processing: {}", requestPayload);
        
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestMessage = (String) request.get("message");
        String requestTopic = (String) request.get("topic");
        
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            "payment-response-" + System.currentTimeMillis(),
            createPaymentResponse(requestMessage),
            "JSON",
            requestTopic + "-payments-processed" // Custom response topic
        );
        
        logger.info("📤 Returning webhook response: key={}, format={}, topic={}", 
                   response.getKey(), response.getResponseFormat(), response.getResponseTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * Generic webhook that echoes back transformed request
     */
    @PostMapping("/echo")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> echoWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.info("📥 Received webhook call for echo: {}", requestPayload);
        
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestTopic = (String) request.get("topic");
        
        String echoContent = String.format(
            "{\"echo\": \"%s\", \"processed_at\": \"%d\"}", 
            request.get("message"), 
            System.currentTimeMillis()
        );
        
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            "echo-" + System.currentTimeMillis(),
            echoContent,
            "JSON",
            null // No custom topic - will use stub or auto-generated
        );
        
        logger.info("📤 Returning webhook response: key={}, format={}, topic={}", 
                   response.getKey(), response.getResponseFormat(), response.getResponseTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook that simulates error scenarios
     */
    @PostMapping("/error")
    public ResponseEntity<Map<String, Object>> errorWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.error("❌ Simulating webhook error");
        return ResponseEntity.status(500).body(Map.of("error", "Simulated webhook error"));
    }

    /**
     * Webhook with delay simulation
     */
    @PostMapping("/slow")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> slowWebhook(@RequestBody Map<String, Object> requestPayload) throws InterruptedException {
        logger.info("⏱️ Simulating slow webhook processing...");
        Thread.sleep(3000); // 3 second delay
        
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestTopic = (String) request.get("topic");
        String slowContent = "{\"status\": \"processed after delay\", \"delay_ms\": 3000}";
        
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            "slow-response-" + System.currentTimeMillis(),
            slowContent,
            "JSON",
            requestTopic + "-slow-responses" // Custom slow response topic
        );
        
        logger.info("📤 Returning delayed webhook response: key={}, format={}, topic={}", 
                   response.getKey(), response.getResponseFormat(), response.getResponseTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook that returns AVRO format response
     */
    @PostMapping("/avro-response")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> avroWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.info("📥 Received webhook call for AVRO processing: {}", requestPayload);
        
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestMessage = (String) request.get("message");
        String requestTopic = (String) request.get("topic");
        
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            "avro-response-" + System.currentTimeMillis(),
            createAvroResponse(requestMessage),
            "AVRO",
            requestTopic + "-avro-processed" // AVRO-specific topic
        );
        
        logger.info("📤 Returning AVRO webhook response: key={}, format={}, topic={}", 
                   response.getKey(), response.getResponseFormat(), response.getResponseTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook that returns response without key (to test auto-generation)
     */
    @PostMapping("/no-key")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> noKeyWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.info("📥 Received webhook call for no-key testing: {}", requestPayload);
        
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestMessage = (String) request.get("message");
        
        // No key provided - should trigger auto-generation
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            null, // No key - will be auto-generated
            "{\"message\": \"Response without key\", \"original\": \"" + requestMessage + "\"}",
            "JSON",
            null // No custom topic - will use stub or auto-generated
        );
        
        logger.info("📤 Returning webhook response without key (will be auto-generated)");
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook that routes to different topics based on message content
     */
    @PostMapping("/topic-router")
    public ResponseEntity<KafkaCallbackService.CallbackResponse> topicRouterWebhook(@RequestBody Map<String, Object> requestPayload) {
        logger.info("📥 Received webhook call for topic routing: {}", requestPayload);
        
        Map<String, Object> request = (Map<String, Object>) requestPayload.get("request");
        String requestMessage = (String) request.get("message");
        String requestTopic = (String) request.get("topic");
        
        // Route to different topics based on message content
        String responseTopic;
        String responseContent;
        
        if (requestMessage.contains("error") || requestMessage.contains("failed")) {
            responseTopic = requestTopic + "-errors";
            responseContent = "{\"status\": \"ERROR\", \"message\": \"Processing failed\", \"original\": \"" + requestMessage + "\"}";
        } else if (requestMessage.contains("urgent") || requestMessage.contains("priority")) {
            responseTopic = requestTopic + "-priority";
            responseContent = "{\"status\": \"PRIORITY\", \"message\": \"Urgent processing completed\", \"original\": \"" + requestMessage + "\"}";
        } else {
            responseTopic = requestTopic + "-standard";
            responseContent = "{\"status\": \"SUCCESS\", \"message\": \"Standard processing completed\", \"original\": \"" + requestMessage + "\"}";
        }
        
        KafkaCallbackService.CallbackResponse response = new KafkaCallbackService.CallbackResponse(
            "routed-" + System.currentTimeMillis(),
            responseContent,
            "JSON",
            responseTopic
        );
        
        logger.info("📤 Returning routed webhook response: key={}, format={}, topic={}", 
                   response.getKey(), response.getResponseFormat(), response.getResponseTopic());
        return ResponseEntity.ok(response);
    }

    /**
     * Create order response based on request content
     */
    private String createOrderResponse(String requestMessage) {
        // Simple order processing simulation
        return String.format("""
        {
            "orderId": "ORD-%d",
            "status": "PROCESSED",
            "message": "Order has been successfully processed",
            "originalRequest": "%s",
            "processedAt": "%d"
        }
        """, System.currentTimeMillis(), requestMessage, System.currentTimeMillis());
    }

    /**
     * Create payment response based on request content
     */
    private String createPaymentResponse(String requestMessage) {
        // Simple payment processing simulation
        return String.format("""
        {
            "paymentId": "PAY-%d",
            "status": "APPROVED",
            "amount": 100.00,
            "currency": "USD",
            "message": "Payment processed successfully",
            "originalRequest": "%s",
            "processedAt": "%d"
        }
        """, System.currentTimeMillis(), requestMessage, System.currentTimeMillis());
    }

    /**
     * Create AVRO response based on request content
     */
    private String createAvroResponse(String requestMessage) {
        // AVRO format simulation
        return String.format("""
        {
            "namespace": "com.example.avro",
            "type": "record",
            "name": "ProcessedMessage",
            "fields": [
                {"name": "id", "type": "string", "value": "AVRO-%d"},
                {"name": "status", "type": "string", "value": "PROCESSED"},
                {"name": "originalMessage", "type": "string", "value": "%s"},
                {"name": "timestamp", "type": "long", "value": %d}
            ]
        }
        """, System.currentTimeMillis(), requestMessage, System.currentTimeMillis());
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Kafka Webhook Test"));
    }
} 
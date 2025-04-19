package com.service.virtualization.activemq.service;

import com.service.virtualization.activemq.model.ActiveMQStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for handling webhook interactions for dynamic responses.
 */
@Service
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Get a response from a webhook URL.
     *
     * @param stub The ActiveMQ stub with webhook configuration
     * @param messageContent The original message content
     * @param headers Headers from the original message
     * @return The response content from the webhook
     */
    public String getWebhookResponse(ActiveMQStub stub, String messageContent, Map<String, String> headers) {
        try {
            String webhookUrl = stub.getWebhookUrl();
            logger.info("Calling webhook URL: {}", webhookUrl);
            
            // Set up headers for the webhook call
            HttpHeaders httpHeaders = new HttpHeaders();
            
            // Add message headers as HTTP headers
            headers.forEach((name, value) -> {
                // Skip headers that would cause issues in HTTP
                if (!name.startsWith("JMS") && !name.contains("-")) {
                    httpHeaders.add("X-JMS-" + name, value);
                }
            });
            
            // Add stub information
            httpHeaders.add("X-Stub-ID", stub.getId());
            httpHeaders.add("X-Destination", stub.getDestinationName());
            
            // Create the request entity with the original message as the body
            HttpEntity<String> requestEntity = new HttpEntity<>(messageContent, httpHeaders);
            
            // Make the request
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                logger.info("Webhook response received successfully");
                return responseBody != null ? responseBody : "";
            } else {
                logger.warn("Webhook returned non-success status: {}", response.getStatusCode());
                return stub.getResponseContent(); // Fall back to static content
            }
        } catch (Exception e) {
            logger.error("Error calling webhook: {}", e.getMessage(), e);
            return stub.getResponseContent(); // Fall back to static content on error
        }
    }
} 
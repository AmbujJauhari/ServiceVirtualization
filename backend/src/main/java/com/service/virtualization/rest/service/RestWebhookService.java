package com.service.virtualization.rest.service;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * Service for REST webhook operations
 * Only active when rest-disabled profile is NOT active
 */
@Service
@Profile("!rest-disabled")
public class RestWebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(RestWebhookService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Call a webhook URL with the original request data
     *
     * @param webhookUrl The webhook URL to call
     * @param originalRequest The original request from WireMock
     * @param stubId The stub ID for context
     * @return The response content from the webhook, or null if failed
     */
    public String callWebhook(String webhookUrl, Request originalRequest, String stubId) {
        try {
            logger.info("Calling webhook URL: {} for stub: {}", webhookUrl, stubId);
            
            // Set up headers for the webhook call
            HttpHeaders httpHeaders = new HttpHeaders();
            
            // Add original request headers, excluding some that might cause issues
            if (originalRequest.getHeaders() != null) {
                for (HttpHeader header : originalRequest.getHeaders().all()) {
                    // Skip headers that would cause issues in HTTP or are WireMock specific
                    if (!shouldSkipHeader(header.key()) && !header.values().isEmpty()) {
                        httpHeaders.add("X-Original-" + header.key(), header.firstValue());
                    }
                }
            }
            
            // Add request context information
            httpHeaders.add("X-Stub-ID", stubId != null ? stubId : "unknown");
            httpHeaders.add("X-Original-Method", originalRequest.getMethod().getName());
            httpHeaders.add("X-Original-URL", originalRequest.getUrl());
            
            // Add query parameters if any
            String queryString = originalRequest.getUrl();
            if (queryString != null && queryString.contains("?")) {
                httpHeaders.add("X-Original-Query", queryString.substring(queryString.indexOf("?") + 1));
            }
            
            // Get the request body
            String requestBody = originalRequest.getBodyAsString();
            
            // Create the request entity with the original request body
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            
            // Make the POST request to webhook
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                logger.info("Webhook response received successfully from: {}", webhookUrl);
                return responseBody != null ? responseBody : "";
            } else {
                logger.warn("Webhook returned non-success status: {} from URL: {}", 
                           response.getStatusCode(), webhookUrl);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error calling webhook URL {}: {}", webhookUrl, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if a header should be skipped when forwarding to webhook
     */
    private boolean shouldSkipHeader(String headerName) {
        if (headerName == null) {
            return true;
        }
        
        String lowerName = headerName.toLowerCase();
        
        // Skip common headers that might cause issues
        return lowerName.startsWith("host") ||
               lowerName.startsWith("content-length") ||
               lowerName.startsWith("connection") ||
               lowerName.startsWith("accept-encoding") ||
               lowerName.startsWith("user-agent") ||
               lowerName.contains("wiremock") ||
               lowerName.contains("x-forwarded");
    }
    
    /**
     * Call webhook with a simple request (for backward compatibility)
     */
    public String callWebhook(String webhookUrl, String method, String url, Map<String, String> headers, String body) {
        try {
            logger.info("Calling webhook URL: {}", webhookUrl);
            
            // Set up headers for the webhook call
            HttpHeaders httpHeaders = new HttpHeaders();
            
            // Add original headers with prefix
            if (headers != null) {
                headers.forEach((name, value) -> {
                    if (!shouldSkipHeader(name)) {
                        httpHeaders.add("X-Original-" + name, value);
                    }
                });
            }
            
            // Add request context
            httpHeaders.add("X-Original-Method", method);
            httpHeaders.add("X-Original-URL", url);
            
            // Create the request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(body, httpHeaders);
            
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
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error calling webhook: {}", e.getMessage(), e);
            return null;
        }
    }
} 
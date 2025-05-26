package com.service.virtualization.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test webhook controller for testing webhook functionality.
 * This controller can receive any HTTP request and log the details.
 */
@RestController
@RequestMapping("/test-webhook")
public class TestWebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestWebhookController.class);
    
    // In-memory storage for received requests (for testing purposes)
    private final Map<String, List<WebhookRequest>> receivedRequests = new ConcurrentHashMap<>();
    
    /**
     * Catch-all endpoint that accepts any HTTP method and logs the request details
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, 
                                            RequestMethod.PUT, RequestMethod.DELETE, 
                                            RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        
        String method = request.getMethod();
        String path = request.getRequestURI();
        String stubId = request.getHeader("X-Stub-ID");
        
        logger.info("Received webhook: {} {} from stub: {}", method, path, stubId);
        
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        
        // Extract query parameters
        Map<String, String> queryParams = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                queryParams.put(key, values[0]);
            }
        });
        
        // Create webhook request record
        WebhookRequest webhookRequest = new WebhookRequest(
                LocalDateTime.now(),
                method,
                path,
                headers,
                queryParams,
                body,
                stubId
        );
        
        // Store the request (for potential inspection)
        String key = stubId != null ? stubId : "unknown";
        receivedRequests.computeIfAbsent(key, k -> new ArrayList<>()).add(webhookRequest);
        
        // Log request details
        logger.info("Webhook request details - Method: {}, Path: {}, Body: {}, Headers: {}", 
                   method, path, body, headers);
        
        // Create dynamic response based on the request
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Webhook received successfully");
        response.put("receivedMethod", method);
        response.put("receivedPath", path);
        response.put("stubId", stubId);
        
        // Add some dynamic content based on request
        if (body != null && !body.isEmpty()) {
            response.put("processedBody", "Processed: " + body);
        }
        
        if (!queryParams.isEmpty()) {
            response.put("processedParams", "Received params: " + queryParams);
        }
        
        // Simulate different response types based on path
        if (path.contains("error")) {
            response.put("status", "error");
            response.put("errorMessage", "Simulated error response");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else if (path.contains("json")) {
            response.put("status", "success");
            response.put("data", Map.of("id", UUID.randomUUID().toString(), "value", "test"));
            return ResponseEntity.ok(response);
        } else if (path.contains("text")) {
            // Return plain text response
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain")
                    .body(Map.of("text", "This is a plain text response from webhook"));
        } else {
            // Default JSON response
            response.put("status", "success");
            response.put("webhook", "active");
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Get history of received webhook requests
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getWebhookHistory(
            @RequestParam(required = false) String stubId) {
        
        Map<String, Object> history = new HashMap<>();
        
        if (stubId != null) {
            List<WebhookRequest> requests = receivedRequests.getOrDefault(stubId, new ArrayList<>());
            history.put("stubId", stubId);
            history.put("requestCount", requests.size());
            history.put("requests", requests);
        } else {
            int totalRequests = receivedRequests.values().stream()
                    .mapToInt(List::size).sum();
            history.put("totalRequests", totalRequests);
            history.put("stubCount", receivedRequests.size());
            history.put("requestsByStub", receivedRequests);
        }
        
        return ResponseEntity.ok(history);
    }
    
    /**
     * Clear webhook history
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory() {
        receivedRequests.clear();
        logger.info("Webhook history cleared");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Webhook history cleared successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple record to store webhook request details
     */
    public record WebhookRequest(
            LocalDateTime timestamp,
            String method,
            String path,
            Map<String, String> headers,
            Map<String, String> queryParams,
            String body,
            String stubId
    ) {}
} 
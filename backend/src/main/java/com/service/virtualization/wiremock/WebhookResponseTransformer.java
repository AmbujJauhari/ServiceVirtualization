package com.service.virtualization.wiremock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.service.virtualization.rest.service.RestWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * WireMock response transformer that handles webhook calls for dynamic responses
 */
@Component
public class WebhookResponseTransformer extends ResponseDefinitionTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookResponseTransformer.class);
    public static final String TRANSFORMER_NAME = "webhook-response-transformer";
    
    @Autowired
    private RestWebhookService restWebhookService;
    
    /**
     * Static instance to handle the webhook service injection
     * This is needed because WireMock creates transformer instances directly
     */
    private static RestWebhookService staticWebhookService;
    
    public WebhookResponseTransformer() {
        // Default constructor required by WireMock
    }
    
    public WebhookResponseTransformer(RestWebhookService restWebhookService) {
        this.restWebhookService = restWebhookService;
        staticWebhookService = restWebhookService;
    }
    
    @Override
    public String getName() {
        return TRANSFORMER_NAME;
    }
    
    @Override
    public boolean applyGlobally() {
        return false; // Only apply to specific stubs that request it
    }
    
    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, 
                                      FileSource files, Parameters parameters) {
        
        logger.debug("Webhook transformer processing request: {} {}", request.getMethod(), request.getUrl());
        
        try {
            // Get webhook parameters
            String webhookUrl = parameters.getString("webhookUrl");
            String stubId = parameters.getString("stubId");
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                logger.warn("Webhook URL not provided in transformer parameters, returning original response");
                return responseDefinition;
            }
            
            logger.info("Processing webhook request for stub {} with URL: {}", stubId, webhookUrl);
            
            // Use the webhook service to make the call
            RestWebhookService webhookService = this.restWebhookService != null ? 
                this.restWebhookService : staticWebhookService;
                
            if (webhookService == null) {
                logger.error("RestWebhookService not available, returning original response");
                return responseDefinition;
            }
            
            // Call webhook and get response
            String webhookResponse = webhookService.callWebhook(webhookUrl, request, stubId);
            
            if (webhookResponse != null) {
                logger.info("Webhook call successful, returning dynamic response");
                
                // Create new response with webhook content
                return ResponseDefinitionBuilder.like(responseDefinition)
                    .withBody(webhookResponse)
                    .withHeader("X-Webhook-Response", "true")
                    .withHeader("X-Stub-ID", stubId != null ? stubId : "unknown")
                    .build();
            } else {
                logger.warn("Webhook call failed, returning original response");
                return responseDefinition;
            }
            
        } catch (Exception e) {
            logger.error("Error in webhook transformer: {}", e.getMessage(), e);
            
            // Return original response on any error
            return ResponseDefinitionBuilder.like(responseDefinition)
                .withHeader("X-Webhook-Error", "true")
                .withHeader("X-Error-Message", e.getMessage())
                .build();
        }
    }
    
    /**
     * Set the static webhook service instance
     * This method is called during Spring context initialization
     */
    public static void setStaticWebhookService(RestWebhookService webhookService) {
        staticWebhookService = webhookService;
    }
} 
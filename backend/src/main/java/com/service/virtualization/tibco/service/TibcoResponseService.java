package com.service.virtualization.tibco.service;

import com.service.virtualization.tibco.config.TibcoServerRegistry;
import com.service.virtualization.tibco.model.TibcoStub;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling responses to matched Tibco messages.
 * Supports multi-server TIBCO configuration.
 * Only active when tibco-disabled profile is NOT active
 */
@Service
@Profile("!tibco-disabled")
public class TibcoResponseService {
    private static final Logger logger = LoggerFactory.getLogger(TibcoResponseService.class);
    @Autowired
    private TibcoServerRegistry serverRegistry;

    @Autowired
    private TibcoWebhookService tibcoWebhookService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    // Cache of JMS templates per server
    private final Map<String, JmsTemplate> queueTemplates = new ConcurrentHashMap<>();
    private final Map<String, JmsTemplate> topicTemplates = new ConcurrentHashMap<>();

    /**
     * Process and send a response for a matched message.
     *
     * @param stub           The matched stub
     * @param message        The original JMS message
     * @param messageContent The content of the original message
     */
    public void processResponse(TibcoStub stub, Message message, String messageContent) {
        try {
            String responseDestination = stub.getResponseDestination();
            String responseDestinationType = stub.getResponseType();

            // Process the response based on type
            final Map<String, String> headers = extractHeaders(message);

            if (stub.getLatency() != null && stub.getLatency() > 0) {
                scheduler.schedule(
                    () -> sendResponse(stub, responseDestination, responseDestinationType, messageContent, headers),
                    stub.getLatency(), TimeUnit.MILLISECONDS);
            } else {
                sendResponse(stub, responseDestination, responseDestinationType, messageContent, headers);
            }
        } catch (Exception e) {
            logger.error("Error processing response for stub {}: {}",
                    stub.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send the response based on the stub configuration.
     *
     * @param stub                   The matched stub
     * @param destination            The destination to send the response to
     * @param destinationType        The type of destination (queue or topic)
     * @param originalMessageContent The content of the original message
     * @param headers                Headers from the original message
     */
    private void sendResponse(TibcoStub stub, String destination, String destinationType,
                              String originalMessageContent, Map<String, String> headers) {
        try {
            // Determine which JmsTemplate to use based on destination type and server
            boolean isTopic = "topic".equalsIgnoreCase(destinationType);
            String responseServerName = stub.getResponseServerName();
            
            JmsTemplate jmsTemplate = getJmsTemplate(responseServerName, isTopic);

            logger.debug("Sending response to {} {} on server '{}' for stub {}",
                    isTopic ? "topic" : "queue", destination, 
                    responseServerName != null ? responseServerName : "default", stub.getId());

            // Check if we should get content from webhook
            String responseContent;
            if (stub.getWebhookUrl() != null && !stub.getWebhookUrl().trim().isEmpty()) {
                responseContent = tibcoWebhookService.getWebhookResponse(stub, originalMessageContent, headers);
            } else {
                responseContent = stub.getResponseContent();
            }

            if (responseContent == null) {
                responseContent = ""; // Default to empty string if null
            }

            // Send the response
            String finalResponseContent = responseContent;
            jmsTemplate.send(destination, session -> {
                TextMessage responseMessage = session.createTextMessage(finalResponseContent);

                // Set correlation ID from original message if available
                try {
                    if (headers.containsKey("JMSCorrelationID")) {
                        responseMessage.setJMSCorrelationID(headers.get("JMSCorrelationID"));
                    } else if (headers.containsKey("JMSMessageID")) {
                        responseMessage.setJMSCorrelationID(headers.get("JMSMessageID"));
                    } else {
                        responseMessage.setJMSCorrelationID(UUID.randomUUID().toString());
                    }
                } catch (JMSException e) {
                    logger.warn("Error setting correlation ID: {}", e.getMessage());
                }

                // Add any custom headers from the stub
                if (stub.getHeaders() != null) {
                    for (Map.Entry<String, String> header : stub.getHeaders().entrySet()) {
                        try {
                            responseMessage.setStringProperty(header.getKey(), header.getValue());
                        } catch (JMSException e) {
                            logger.warn("Error setting property {}: {}", header.getKey(), e.getMessage());
                        }
                    }
                }

                return responseMessage;
            });

            logger.info("Sent response to {} {} for stub {}",
                    isTopic ? "topic" : "queue", destination, stub.getId());
        } catch (Exception e) {
            logger.error("Error sending response: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract headers from a JMS message.
     *
     * @param message The JMS message
     * @return Map of header names and values
     */
    private Map<String, String> extractHeaders(Message message) throws JMSException {
        Map<String, String> headers = new java.util.HashMap<>();

        // Get all properties from the message
        java.util.Enumeration<?> propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            String value = message.getStringProperty(name);
            headers.put(name, value);
        }

        // Add JMS-specific headers
        headers.put("JMSMessageID", message.getJMSMessageID());

        if (message.getJMSCorrelationID() != null) {
            headers.put("JMSCorrelationID", message.getJMSCorrelationID());
        }

        return headers;
    }

    /**
     * Gets or creates a JmsTemplate for the specified server and destination type.
     * 
     * @param serverName The server name (null for default)
     * @param isTopic True for topic, false for queue
     * @return JmsTemplate for the server
     */
    private JmsTemplate getJmsTemplate(String serverName, boolean isTopic) {
        Map<String, JmsTemplate> templateCache = isTopic ? topicTemplates : queueTemplates;
        // ConcurrentHashMap does not permit null keys; use a sentinel for single-server mode.
        // TibcoServerRegistry.getConnectionFactory(null) resolves to the default factory.
        String cacheKey = (serverName != null && !serverName.trim().isEmpty()) ? serverName.trim() : "__default__";
        return templateCache.computeIfAbsent(cacheKey, name -> {
            String registryKey = "__default__".equals(name) ? null : name;
            ConnectionFactory connectionFactory = serverRegistry.getConnectionFactory(registryKey);
            JmsTemplate template = new JmsTemplate();
            template.setConnectionFactory(connectionFactory);
            template.setPubSubDomain(isTopic);
            logger.debug("Created {} JmsTemplate for server '{}'", isTopic ? "topic" : "queue", cacheKey);
            return template;
        });
    }
} 

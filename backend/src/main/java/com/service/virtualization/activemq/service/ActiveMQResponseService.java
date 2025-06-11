package com.service.virtualization.activemq.service;

import com.service.virtualization.activemq.model.ActiveMQStub;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for processing ActiveMQ response generation
 * Only active when activemq-disabled profile is NOT active
 */
@Service
@Profile("!activemq-disabled")
public class ActiveMQResponseService {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQResponseService.class);

    @Autowired
    @Qualifier("activemqQueueJmsTemplate")
    private JmsTemplate queueJmsTemplate;

    @Autowired
    @Qualifier("activemqTopicJmsTemplate")
    private JmsTemplate topicJmsTemplate;

    @Autowired
    private ActiveMQWebhookService activeMQWebhookService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    /**
     * Process and send a response for a matched message.
     *
     * @param stub           The matched stub
     * @param message        The original JMS message
     * @param messageContent The content of the original message
     */
    public void processResponse(ActiveMQStub stub, Message message, String messageContent) {
        try {
            String responseDestination = stub.getResponseDestination();
            String responseDestinationType = stub.getResponseType();

            // If no response destination is specified, use the JMSReplyTo if available
            if (responseDestination == null || responseDestination.trim().isEmpty()) {
                if (message.getJMSReplyTo() != null) {
                    responseDestination = message.getJMSReplyTo().toString();
                    // Try to determine destination type from JMSReplyTo
                    responseDestinationType = determineDestinationTypeFromReplyTo(message.getJMSReplyTo().toString());
                } else {
                    logger.warn("No response destination specified and no JMSReplyTo in message for stub {}",
                            stub.getId());
                    return;
                }
            }

            // Process the response based on type
            final String finalDestination = responseDestination;
            final String finalDestinationType = responseDestinationType;
            final Map<String, String> headers = extractHeaders(message);

            // Handle latency if specified
            if (stub.getLatency() > 0) {
                scheduler.schedule(() -> {
                    sendResponse(stub, finalDestination, finalDestinationType, messageContent, headers);
                }, stub.getLatency(), TimeUnit.MILLISECONDS);
            } else {
                sendResponse(stub, finalDestination, finalDestinationType, messageContent, headers);
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
    private void sendResponse(ActiveMQStub stub, String destination, String destinationType,
                              String originalMessageContent, Map<String, String> headers) {
        try {
            // Determine which JmsTemplate to use based on destination type
            boolean isTopic = "topic".equalsIgnoreCase(destinationType);
            JmsTemplate jmsTemplate = isTopic ? topicJmsTemplate : queueJmsTemplate;

            logger.debug("Sending response to {} {} for stub {}", 
                    isTopic ? "topic" : "queue", destination, stub.getId());

            // Check if we should get content from webhook
            String responseContent;
            if (stub.getWebhookUrl() != null && !stub.getWebhookUrl().trim().isEmpty()) {
                responseContent = activeMQWebhookService.getWebhookResponse(stub, originalMessageContent, headers);
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
     * Try to determine destination type from JMSReplyTo string.
     * This is a best-effort approach as the format may vary.
     */
    private String determineDestinationTypeFromReplyTo(String replyTo) {
        if (replyTo == null) {
            return "queue"; // Default to queue
        }
        
        if (replyTo.toLowerCase().contains("topic")) {
            return "topic";
        } else {
            return "queue";
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
} 
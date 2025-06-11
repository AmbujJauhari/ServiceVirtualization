package com.service.virtualization.tibco.service;

import com.service.virtualization.tibco.model.TibcoStub;
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

/**
 * Service for handling responses to matched Tibco messages.
 * Only active when tibco-disabled profile is NOT active
 */
@Service
@Profile("!tibco-disabled")
public class TibcoResponseService {
    private static final Logger logger = LoggerFactory.getLogger(TibcoResponseService.class);

    @Autowired
    @Qualifier("tibcoQueueJmsTemplate")
    private JmsTemplate queueJmsTemplate;

    @Autowired
    @Qualifier("tibcoTopicJmsTemplate")
    private JmsTemplate topicJmsTemplate;

    @Autowired
    private TibcoWebhookService TibcoWebhookService;

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

            sendResponse(stub, responseDestination, responseDestinationType, messageContent, headers);
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
            // Determine which JmsTemplate to use based on destination type
            boolean isTopic = "topic".equalsIgnoreCase(destinationType);
            JmsTemplate jmsTemplate = isTopic ? topicJmsTemplate : queueJmsTemplate;

            logger.debug("Sending response to {} {} for stub {}",
                    isTopic ? "topic" : "queue", destination, stub.getId());

            // Check if we should get content from webhook
            String responseContent;
            if (stub.getWebhookUrl() != null && !stub.getWebhookUrl().trim().isEmpty()) {
                responseContent = TibcoWebhookService.getWebhookResponse(stub, originalMessageContent, headers);
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
} 
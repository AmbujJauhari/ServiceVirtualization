package com.service.virtualization.tibco.listener;

import com.service.virtualization.tibco.matcher.TibcoStubMatcher;
import com.service.virtualization.tibco.model.TibcoStub;
import com.service.virtualization.tibco.service.TibcoResponseService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message listener for Tibco messages.
 * Handles incoming messages, matches them against registered stubs,
 * and processes responses according to stub configuration.
 */
@Component
public class TibcoMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(TibcoMessageListener.class);

    @Autowired
    private TibcoStubMatcher tibcoStubMatcher;
    
    @Autowired
    private TibcoResponseService responseService;
    
    // Map to store stubs by ID
    private final Map<String, TibcoStub> registeredStubs = new ConcurrentHashMap<>();
    
    /**
     * Handles incoming JMS messages.
     *
     * @param message The JMS message
     */
    @Override
    public void onMessage(Message message) {
        try {
            String destinationName = getDestinationName(message.getJMSDestination());
            logger.debug("Received message from {}", destinationName);
            
            // Extract message content for matching
            String messageContent = extractMessageContent(message);
            
            // Find matching stub
            TibcoStub matchingStub = tibcoStubMatcher.findMatchingStub(message, registeredStubs.values());
            
            if (matchingStub != null) {
                logger.info("Found matching stub {} for message on {}", 
                        matchingStub.getId(), destinationName);
                
                // Process and send response
                responseService.processResponse(matchingStub, message, messageContent);
            } else {
                logger.warn("No matching stub found for message on {}: {}", 
                        destinationName, messageContent);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Register a stub with this listener.
     *
     * @param stub The stub to register
     */
    public void registerStub(TibcoStub stub) {
        registeredStubs.put(stub.getId(), stub);
        logger.info("Registered stub {} for {}", stub.getId(), stub.getDestinationName());
    }
    
    /**
     * Unregister a stub from this listener.
     *
     * @param stubId The ID of the stub to unregister
     */
    public void unregisterStub(String stubId) {
        registeredStubs.remove(stubId);
        logger.info("Unregistered stub {}", stubId);
    }
    
    /**
     * Get the destination name from a JMS Destination object.
     *
     * @param destination The JMS Destination object
     * @return The destination name
     */
    private String getDestinationName(jakarta.jms.Destination destination) {
        try {
            if (destination instanceof jakarta.jms.Queue) {
                return ((jakarta.jms.Queue) destination).getQueueName();
            } else if (destination instanceof jakarta.jms.Topic) {
                return ((jakarta.jms.Topic) destination).getTopicName();
            } else {
                return destination.toString();
            }
        } catch (Exception e) {
            logger.warn("Error getting destination name: {}", e.getMessage());
            return destination.toString();
        }
    }

    /**
     * Extract content from a JMS message.
     *
     * @param message The JMS message
     * @return The message content as a string
     * @throws JMSException If there is an error accessing message content
     */
    private String extractMessageContent(Message message) throws JMSException {
        try {
            // Handle our Jakarta-to-Javax adapter specifically
            if (message.getClass().getName().contains("JavaxToJakartaMessageAdapter")) {
                // Use getBody method to get text content from our adapter
                try {
                    return message.getBody(String.class);
                } catch (Exception e) {
                    logger.debug("Could not get body as String, trying other methods: {}", e.getMessage());
                }
            }
            
            // Standard TextMessage handling
            if (message instanceof TextMessage) {
                return ((TextMessage) message).getText();
            } else {
                // Try to get the body as String for JMS 2.0 messages
                try {
                    return message.getBody(String.class);
                } catch (Exception e) {
                    logger.debug("Could not extract body as String: {}", e.getMessage());
                    return message.toString();
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting message content: {}", e.getMessage());
            return "";
        }
    }
} 
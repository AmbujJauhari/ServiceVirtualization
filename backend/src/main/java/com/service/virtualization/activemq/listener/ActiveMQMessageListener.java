package com.service.virtualization.activemq.listener;

import com.service.virtualization.activemq.matcher.StubMatcher;
import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.service.ActiveMQResponseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message listener for ActiveMQ messages.
 * Handles incoming messages, matches them against registered stubs,
 * and processes responses according to stub configuration.
 */
@Component
public class ActiveMQMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQMessageListener.class);

    @Autowired
    private StubMatcher stubMatcher;
    
    @Autowired
    private ActiveMQResponseService responseService;
    
    // Map to store stubs by ID
    private final Map<String, ActiveMQStub> registeredStubs = new ConcurrentHashMap<>();
    
    /**
     * Handles incoming JMS messages.
     *
     * @param message The JMS message
     */
    @Override
    public void onMessage(Message message) {
        try {
            String destinationName = message.getJMSDestination().toString();
            logger.debug("Received message from {}", destinationName);
            
            // Extract message content for matching
            String messageContent = extractMessageContent(message);
            
            // Find matching stub
            ActiveMQStub matchingStub = stubMatcher.findMatchingStub(message, registeredStubs.values());
            
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
    public void registerStub(ActiveMQStub stub) {
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
     * Extract content from a JMS message.
     *
     * @param message The JMS message
     * @return The message content as a string
     * @throws JMSException If there is an error accessing message content
     */
    private String extractMessageContent(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            return ((TextMessage) message).getText();
        } else {
            return message.toString();
        }
    }
} 
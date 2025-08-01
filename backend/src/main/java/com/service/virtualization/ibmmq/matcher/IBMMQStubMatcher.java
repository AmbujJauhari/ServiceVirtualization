package com.service.virtualization.ibmmq.matcher;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.model.StubStatus;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Handles matching of JMS messages against registered IBMMQ stubs.
 */
@Component
public class IBMMQStubMatcher {
    private static final Logger logger = LoggerFactory.getLogger(IBMMQStubMatcher.class);
    
    /**
     * Find a matching stub for the given message.
     *
     * @param message The JMS message to match
     * @param registeredStubs Collection of registered stubs to match against
     * @return The matching stub, or null if no match is found
     */
    public IBMMQStub findMatchingStub(Message message, Collection<IBMMQStub> registeredStubs) {
        if (registeredStubs.isEmpty()) {
            return null;
        }
        
        try {
            // Get destination for matching
            String destination = message.getJMSDestination().toString();
            
            // Extract message content for content-based matching
            String messageContent = extractMessageContent(message);
            
            // Find matching stubs and sort by priority (highest first)
            // Only consider active stubs
            // Sort by priority (highest first)
            return registeredStubs.stream()
                    .filter(stub -> isDestinationMatch(stub, destination))
                    .filter(stub -> isMessageSelectorMatch(stub, message))
                    .filter(stub -> isContentMatch(stub, messageContent))
                    .filter(stub -> stub.getStatus() == StubStatus.ACTIVE)
                    .max(Comparator.comparing(IBMMQStub::getPriority))
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error matching message to stub: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if the stub's destination matches the message destination.
     *
     * @param stub The stub to check
     * @param messageDestination The message destination string
     * @return True if the destinations match
     */
    private boolean isDestinationMatch(IBMMQStub stub, String messageDestination) {
        // Extract the actual destination name from format like "queue://QUEUE_NAME"
        String extractedName = extractDestinationName(messageDestination);
        
        // Check for an exact match
        return extractedName.equalsIgnoreCase(stub.getDestinationName());
    }
    
    /**
     * Check if the message matches the stub's selector.
     *
     * @param stub The stub to check
     * @param message The message to check
     * @return True if the message matches the selector
     */
    private boolean isMessageSelectorMatch(IBMMQStub stub, Message message) {
        String selector = stub.getMessageSelector();
        
        // If no selector specified, any message is a match
        if (selector == null || selector.trim().isEmpty()) {
            return true;
        }
        
        try {
            // Handle simple property-based selectors
            if (selector.contains("=")) {
                String[] parts = selector.split("=", 2);
                if (parts.length == 2) {
                    String propertyName = parts[0].trim();
                    String expectedValue = parts[1].trim().replace("'", "").replace("\"", "");
                    
                    String actualValue = message.getStringProperty(propertyName);
                    return expectedValue.equals(actualValue);
                }
            }
            
            // For more complex selectors, we would need a full JMS selector evaluator
            // This is a basic implementation
            return matchAllProperties(message, selector);
        } catch (JMSException e) {
            logger.warn("Error evaluating selector '{}': {}", selector, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the message content matches the stub's content pattern.
     * 
     * @param stub The stub to check
     * @param messageContent The message content as a string
     * @return True if the content matches or if no content matching is configured
     */
    private boolean isContentMatch(IBMMQStub stub, String messageContent) {
        // If no content pattern or match type is NONE, any content is a match
        if (stub.getContentMatchType() == IBMMQStub.ContentMatchType.NONE ||
            stub.getContentPattern() == null || 
            stub.getContentPattern().isEmpty()) {
            return true;
        }
        
        // Get pattern from stub
        String pattern = stub.getContentPattern();
        String content = messageContent;
        
        // Apply case insensitivity if needed
        if (!stub.isCaseSensitive()) {
            pattern = pattern.toLowerCase();
            content = content.toLowerCase();
        }
        
        // Match based on the match type
        switch (stub.getContentMatchType()) {
            case CONTAINS:
                return content.contains(pattern);
            case EXACT:
                return content.equals(pattern);
            case REGEX:
                try {
                    return Pattern.compile(pattern).matcher(content).matches();
                } catch (Exception e) {
                    logger.warn("Invalid regex pattern: {}", pattern, e);
                    return false;
                }
            default:
                return true;
        }
    }
    
    /**
     * Simple property-based matching for all message properties.
     *
     * @param message The message to check
     * @param selectorPattern Pattern to match against properties
     * @return True if any property matches the pattern
     */
    private boolean matchAllProperties(Message message, String selectorPattern) throws JMSException {
        Enumeration<?> propertyNames = message.getPropertyNames();
        Pattern pattern = Pattern.compile(selectorPattern, Pattern.CASE_INSENSITIVE);
        
        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            String value = message.getStringProperty(name);
            
            if (pattern.matcher(value).matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extract the destination name from a JMS destination string.
     *
     * @param destination The full destination string
     * @return The extracted destination name
     */
    private String extractDestinationName(String destination) {
        if (destination == null || destination.isEmpty()) {
            return "";
        }
        
        // Handle IBM MQ queue names that may contain multiple forward slashes
        // e.g., "queue:///QM1/QUEUE_NAME" or "queue://host:port/QM1/QUEUE_NAME"
        // Extract everything after the last forward slash
        int lastSlashIndex = destination.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < destination.length() - 1) {
            String extractedName = destination.substring(lastSlashIndex + 1);
            logger.debug("Extracted destination name '{}' from full destination '{}'", extractedName, destination);
            return extractedName;
        }
        
        // If no slashes found, return the destination as-is
        logger.debug("No slashes found in destination '{}', returning as-is", destination);
        return destination;
    }
    
    /**
     * Extract content from a JMS message.
     *
     * @param message The JMS message
     * @return The message content as a string
     */
    private String extractMessageContent(Message message) {
        try {
            if (message instanceof TextMessage) {
                return ((TextMessage) message).getText();
            } else {
                return message.toString();
            }
        } catch (JMSException e) {
            logger.warn("Error extracting message content: {}", e.getMessage());
            return "";
        }
    }
} 
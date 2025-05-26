package com.service.virtualization.ibmmq.service;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import jakarta.jms.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service that listens for IBM MQ messages and processes them based on configured stubs.
 */
@Service
public class IBMMQStubListenerService {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQStubListenerService.class);

    private final IBMMQStubService stubService;
    private final JmsTemplate jmsTemplate;

    @Autowired
    public IBMMQStubListenerService(IBMMQStubService stubService, 
                                   @Qualifier("ibmMQTemplate") JmsTemplate jmsTemplate) {
        this.stubService = stubService;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Listen for messages on IBM MQ queues and process them with configured stubs.
     * This is a generic listener - in practice, you'd have specific listeners for each queue/stub.
     */
    @JmsListener(destination = "${ibmmq.default.queue:DEV.QUEUE.1}", containerFactory = "ibmmqListenerContainerFactory")
    public void handleMessage(Message message, Session session) {
        try {
            logger.debug("Received IBM MQ message: {}", message.getJMSMessageID());
            
            String queueName = extractQueueName(message);
            String queueManager = extractQueueManager(message);
            
            // Find active stubs for this queue
            List<IBMMQStub> activeStubs = stubService.findActiveByQueueManagerAndName(queueManager, queueName);
            
            if (activeStubs.isEmpty()) {
                logger.debug("No active stubs found for queue '{}' on queue manager '{}'", queueName, queueManager);
                return;
            }
            
            // Sort stubs by priority (highest first)
            activeStubs.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            
            // Find the first matching stub
            for (IBMMQStub stub : activeStubs) {
                if (matchesStub(message, stub)) {
                    logger.info("Processing message with stub: {}", stub.getName());
                    processMessage(message, stub, session);
                    return; // Stop after first match
                }
            }
            
            logger.debug("No matching stubs found for message");
            
        } catch (Exception e) {
            logger.error("Error processing IBM MQ message: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if a message matches a stub's criteria.
     */
    private boolean matchesStub(Message message, IBMMQStub stub) throws JMSException {
        // Check JMS selector first
        if (stub.getSelector() != null && !stub.getSelector().trim().isEmpty()) {
            // Note: JMS selector validation would typically be done by the JMS provider
            // Here we do a simple property-based check for demonstration
            if (!matchesSelector(message, stub.getSelector())) {
                return false;
            }
        }
        
        // Check content matching
        return matchesContent(message, stub);
    }

    /**
     * Check if a message matches the content criteria.
     */
    private boolean matchesContent(Message message, IBMMQStub stub) throws JMSException {
        // If no content matching is configured, match all
        if (stub.getContentMatchType() == null || 
            stub.getContentMatchType() == IBMMQStub.ContentMatchType.NONE ||
            stub.getContentPattern() == null || 
            stub.getContentPattern().trim().isEmpty()) {
            return true;
        }
        
        String messageContent = extractMessageContent(message);
        if (messageContent == null) {
            return false;
        }
        
        return matchesStandardizedContent(messageContent, stub);
    }

    /**
     * Checks if the message content matches the standardized content pattern.
     */
    private boolean matchesStandardizedContent(String messageContent, IBMMQStub stub) {
        String pattern = stub.getContentPattern();
        boolean caseSensitive = stub.isCaseSensitive();
        
        // Apply case sensitivity
        String content = caseSensitive ? messageContent : messageContent.toLowerCase();
        String searchPattern = caseSensitive ? pattern : pattern.toLowerCase();
        
        switch (stub.getContentMatchType()) {
            case CONTAINS:
                return content.contains(searchPattern);
            case EXACT:
                return content.equals(searchPattern);
            case REGEX:
                try {
                    Pattern regexPattern = caseSensitive ? 
                        Pattern.compile(pattern) : 
                        Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    return regexPattern.matcher(messageContent).matches();
                } catch (Exception e) {
                    logger.warn("Invalid regex pattern '{}' for stub {}: {}", pattern, stub.getId(), e.getMessage());
                    return false;
                }
            default:
                return true;
        }
    }

    /**
     * Simple JMS selector matching (basic implementation).
     */
    private boolean matchesSelector(Message message, String selector) throws JMSException {
        // This is a simplified implementation
        // In practice, you'd use the JMS provider's selector evaluation
        try {
            // For demonstration, we'll check if selector contains property checks
            if (selector.contains("=")) {
                String[] parts = selector.split("=");
                if (parts.length == 2) {
                    String propertyName = parts[0].trim();
                    String expectedValue = parts[1].trim().replace("'", "");
                    String actualValue = message.getStringProperty(propertyName);
                    return expectedValue.equals(actualValue);
                }
            }
            return true; // Default to match if selector can't be parsed
        } catch (Exception e) {
            logger.warn("Error evaluating selector '{}': {}", selector, e.getMessage());
            return true; // Default to match on error
        }
    }

    /**
     * Process a message that matches a stub.
     */
    private void processMessage(Message message, IBMMQStub stub, Session session) {
        try {
            // Apply latency if configured
            if (stub.getLatency() != null && stub.getLatency() > 0) {
                Thread.sleep(stub.getLatency());
            }
            
            // Send response if response content is configured
            if (stub.getResponseContent() != null && !stub.getResponseContent().trim().isEmpty()) {
                sendResponse(message, stub, session);
            }
            
            logger.info("Successfully processed message with stub '{}' (ID: {})", stub.getName(), stub.getId());
            
        } catch (Exception e) {
            logger.error("Error processing message with stub '{}': {}", stub.getName(), e.getMessage(), e);
        }
    }

    /**
     * Send a response message.
     */
    private void sendResponse(Message originalMessage, IBMMQStub stub, Session session) throws JMSException {
        // Create response message
        Message responseMessage = createResponseMessage(session, stub);
        
        // Set correlation ID to match original message
        responseMessage.setJMSCorrelationID(originalMessage.getJMSMessageID());
        
        // Send to reply-to destination if specified, otherwise to the same queue
        Destination replyTo = originalMessage.getJMSReplyTo();
        if (replyTo == null) {
            replyTo = session.createQueue(stub.getQueueName());
        }
        
        MessageProducer producer = session.createProducer(replyTo);
        try {
            producer.send(responseMessage);
            logger.debug("Sent response message to destination: {}", replyTo);
        } finally {
            producer.close();
        }
    }

    /**
     * Create a response message based on stub configuration.
     */
    private Message createResponseMessage(Session session, IBMMQStub stub) throws JMSException {
        String responseContent = stub.getResponseContent();
        Message message;
        
        // Determine message type based on response type or content
        String responseType = stub.getResponseType();
        if ("json".equalsIgnoreCase(responseType) || 
            (responseContent.trim().startsWith("{") || responseContent.trim().startsWith("["))) {
            message = session.createTextMessage(responseContent);
            message.setStringProperty("ContentType", "application/json");
        } else if ("xml".equalsIgnoreCase(responseType) || responseContent.trim().startsWith("<")) {
            message = session.createTextMessage(responseContent);
            message.setStringProperty("ContentType", "application/xml");
        } else {
            message = session.createTextMessage(responseContent);
            message.setStringProperty("ContentType", "text/plain");
        }
        
        // Add custom headers
        if (stub.getHeaders() != null) {
            stub.getHeaders().forEach(header -> {
                try {
                    setMessageProperty(message, header.getName(), header.getValue(), header.getType());
                } catch (JMSException e) {
                    logger.warn("Failed to set header '{}': {}", header.getName(), e.getMessage());
                }
            });
        }
        
        return message;
    }

    /**
     * Set a message property with proper type handling.
     */
    private void setMessageProperty(Message message, String name, String value, String type) throws JMSException {
        if (value == null) {
            return;
        }
        
        String propertyType = type != null ? type.toLowerCase() : "string";
        
        switch (propertyType) {
            case "int":
            case "integer":
                message.setIntProperty(name, Integer.parseInt(value));
                break;
            case "long":
                message.setLongProperty(name, Long.parseLong(value));
                break;
            case "double":
                message.setDoubleProperty(name, Double.parseDouble(value));
                break;
            case "boolean":
                message.setBooleanProperty(name, Boolean.parseBoolean(value));
                break;
            default:
                message.setStringProperty(name, value);
        }
    }

    /**
     * Extract message content as string.
     */
    private String extractMessageContent(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            return ((TextMessage) message).getText();
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(bytes);
            return new String(bytes);
        } else if (message instanceof MapMessage) {
            // Convert MapMessage to JSON-like string
            MapMessage mapMessage = (MapMessage) message;
            StringBuilder json = new StringBuilder("{");
            java.util.Enumeration<?> mapNames = mapMessage.getMapNames();
            while (mapNames.hasMoreElements()) {
                String name = (String) mapNames.nextElement();
                Object value = mapMessage.getObject(name);
                json.append("\"").append(name).append("\":\"").append(value).append("\"");
                if (mapNames.hasMoreElements()) {
                    json.append(",");
                }
            }
            json.append("}");
            return json.toString();
        }
        return null;
    }

    /**
     * Extract queue name from message destination.
     */
    private String extractQueueName(Message message) throws JMSException {
        Destination destination = message.getJMSDestination();
        if (destination instanceof Queue) {
            return ((Queue) destination).getQueueName();
        }
        return "UNKNOWN";
    }

    /**
     * Extract queue manager name from message (IBM MQ specific).
     */
    private String extractQueueManager(Message message) throws JMSException {
        try {
            // Try to get queue manager from message properties
            String queueManager = message.getStringProperty("JMS_IBM_MQMDQueueManagerName");
            return queueManager != null ? queueManager : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
} 
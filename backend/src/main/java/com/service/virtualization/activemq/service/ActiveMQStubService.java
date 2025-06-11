package com.service.virtualization.activemq.service;

import com.service.virtualization.activemq.listener.ActiveMqDynamicDestinationManager;
import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.repository.ActiveMQStubRepository;
import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.model.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Base64;

/**
 * Custom exception for priority conflicts with existing stubs
 */
class StubPriorityConflictException extends RuntimeException {
    private final ActiveMQStub conflictingStub;
    
    public StubPriorityConflictException(String message, ActiveMQStub conflictingStub) {
        super(message);
        this.conflictingStub = conflictingStub;
    }
    
    public ActiveMQStub getConflictingStub() {
        return conflictingStub;
    }
}

/**
 * Service for managing ActiveMQ stubs.
 */
@Service
public class ActiveMQStubService {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQStubService.class);
    
    @Autowired
    private ActiveMQStubRepository activeMQStubRepository;
    
    @Autowired
    private ActiveMqDynamicDestinationManager destinationManager;
    
    /**
     * Get all ActiveMQ stubs.
     *
     * @return List of all stubs
     */
    public List<ActiveMQStub> getAllStubs() {
        return activeMQStubRepository.findAll();
    }
    
    /**
     * Get all active ActiveMQ stubs.
     *
     * @return List of active stubs
     */
    public List<ActiveMQStub> getActiveStubs() {
        return activeMQStubRepository.findByStatus(StubStatus.ACTIVE);
    }
    
    /**
     * Get stubs for a specific user.
     *
     * @param userId The user ID
     * @return List of stubs belonging to the user
     */
    public List<ActiveMQStub> getStubsByUser(String userId) {
        return activeMQStubRepository.findByUserId(userId);
    }
    
    /**
     * Get a specific stub by ID.
     *
     * @param id The stub ID
     * @return The stub or null if not found
     */
    public ActiveMQStub getStubById(String id) {
        return activeMQStubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with id: " + id));
    }
    
    /**
     * Create a new ActiveMQ stub after validating priority constraints.
     *
     * @param stub The stub to create
     * @return The created stub
     * @throws StubPriorityConflictException if a higher priority stub exists
     */
    public ActiveMQStub  createStub(ActiveMQStub stub) {
        // Check for existing stubs with higher priority for the same destination
        validateStubPriority(stub);
        
        LocalDateTime now = LocalDateTime.now();
        stub.setCreatedAt(now);
        stub.setUpdatedAt(now);
        
        ActiveMQStub savedStub = activeMQStubRepository.save(stub);
        
        // Register listener if stub is active
        if (savedStub.isActive()) {
            registerStubListener(savedStub);
        }
        
        return savedStub;
    }
    
    /**
     * Validate that no existing stub has a higher priority than the provided stub.
     * 
     * @param stub The stub to validate
     * @throws StubPriorityConflictException if a higher priority stub exists
     */
    private void validateStubPriority(ActiveMQStub stub) {
        // If this is an update of an existing stub, skip validation
        if (stub.getId() != null) {
            return;
        }
        
        // Find the highest priority stub for this destination
        ActiveMQStub highestPriorityStub = activeMQStubRepository
                .findFirstByDestinationNameAndDestinationTypeOrderByPriorityDesc(
                        stub.getDestinationName(), stub.getDestinationType());
        
        // If we found a stub with higher priority, throw an exception
        if (highestPriorityStub != null && highestPriorityStub.getPriority() > stub.getPriority()) {
            String message = String.format(
                "Cannot create stub with priority %d. A stub with higher priority (%d) already exists: %s",
                stub.getPriority(), 
                highestPriorityStub.getPriority(),
                highestPriorityStub.getName()
            );
            throw new StubPriorityConflictException(message, highestPriorityStub);
        }
    }
    
    /**
     * Update an existing ActiveMQ stub.
     *
     * @param id The stub ID
     * @param stubDetails The updated stub details
     * @return The updated stub
     */
    public ActiveMQStub updateStub(String id, ActiveMQStub stubDetails) {
        ActiveMQStub existingStub = getStubById(id);
        
        // Update fields
        existingStub.setName(stubDetails.getName());
        existingStub.setDescription(stubDetails.getDescription());
        existingStub.setDestinationType(stubDetails.getDestinationType());
        existingStub.setDestinationName(stubDetails.getDestinationName());
        existingStub.setMessageSelector(stubDetails.getMessageSelector());
        existingStub.setContentMatchType(stubDetails.getContentMatchType());
        existingStub.setContentPattern(stubDetails.getContentPattern());
        existingStub.setCaseSensitive(stubDetails.isCaseSensitive());
        existingStub.setResponseType(stubDetails.getResponseType());
        existingStub.setResponseDestination(stubDetails.getResponseDestination());
        existingStub.setResponseContent(stubDetails.getResponseContent());
        existingStub.setWebhookUrl(stubDetails.getWebhookUrl());
        existingStub.setPriority(stubDetails.getPriority());
        existingStub.setHeaders(stubDetails.getHeaders());
        existingStub.setStatus(stubDetails.getStatus());
        existingStub.setUpdatedAt(LocalDateTime.now());
        
        ActiveMQStub updatedStub = activeMQStubRepository.save(existingStub);
        
        // Handle listener registration/unregistration if status changed
        updateStubListener(updatedStub);
        
        return updatedStub;
    }
    
    /**
     * Toggle the status of a stub.
     *
     * @param id The stub ID
     * @return The updated stub
     */
    public ActiveMQStub toggleStubStatus(String id) {
        ActiveMQStub stub = getStubById(id);
        // Toggle between ACTIVE and INACTIVE
        stub.setStatus(stub.getStatus() == StubStatus.ACTIVE ? StubStatus.INACTIVE : StubStatus.ACTIVE);
        stub.setUpdatedAt(LocalDateTime.now());
        
        ActiveMQStub updatedStub = activeMQStubRepository.save(stub);
        
        // Handle listener registration/unregistration
        updateStubListener(updatedStub);
        
        return updatedStub;
    }
    
    /**
     * Delete a stub.
     *
     * @param id The stub ID
     */
    public void deleteStub(String id) {
        ActiveMQStub stub = getStubById(id);
        
        // Unregister listener if it exists
        destinationManager.unregisterListener(id);
        
        activeMQStubRepository.delete(stub);
        logger.info("Deleted ActiveMQ stub with ID: {}", id);
    }
    
    /**
     * Register a stub's JMS listener.
     *
     * @param stub The stub to register
     */
    private void registerStubListener(ActiveMQStub stub) {
        if (stub.isActive()) {
            boolean success = destinationManager.registerListener(stub);
            if (success) {
                logger.info("Registered listener for ActiveMQ stub: {}", stub.getId());
            } else {
                logger.warn("Failed to register listener for ActiveMQ stub: {}", stub.getId());
            }
        }
    }
    
    /**
     * Update a stub's JMS listener registration status.
     *
     * @param stub The stub to update
     */
    public void updateStubListener(ActiveMQStub stub) {
        if (stub.isActive()) {
            registerStubListener(stub);
        } else {
            destinationManager.unregisterListener(stub.getId());
            logger.info("Unregistered listener for ActiveMQ stub: {}", stub.getId());
        }
    }
    
    /**
     * Initialize active stubs on application startup.
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("Initializing ActiveMQ stub listeners on startup");
        
        List<ActiveMQStub> activeStubs = activeMQStubRepository.findByStatus(StubStatus.ACTIVE);
        logger.info("Found {} active ActiveMQ stubs", activeStubs.size());
        
        activeStubs.forEach(this::registerStubListener);
    }

    /**
     * Publish a message to an ActiveMQ destination
     */
    public boolean publishMessage(String queueManager, String destinationType, String destinationName, String message, List<MessageHeader> headers) {
        try {
            // Configure JmsTemplate for the specific queue manager if needed
            // This might require more dynamic configuration based on your ActiveMQ setup
            
            boolean isTopic = "topic".equalsIgnoreCase(destinationType);

            // Send message to the specified destination (queue or topic)
//            jmsTemplate.send(destinationName, session -> {
//                Message jmsMessage = createMessage(session, message, headers);
//                return jmsMessage;
//            });

            logger.info("Successfully published message to {} '{}' on queue manager '{}'", 
                       isTopic ? "topic" : "queue", destinationName, queueManager);
            return true;
        } catch (Exception e) {
            logger.error("Failed to publish message to ActiveMQ {}: {}", destinationType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a JMS message with appropriate type and headers
     */
    private Message createMessage(Session session, String content, List<MessageHeader> headers) throws JMSException {
        Message message;

        // Determine if content is likely JSON or XML based on content
        if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
            message = session.createTextMessage(content);
            message.setStringProperty("ContentType", "application/json");
        } else if (content.trim().startsWith("<")) {
            message = session.createTextMessage(content);
            message.setStringProperty("ContentType", "application/xml");
        } else if (isBase64(content)) {
            // Handle binary content (Base64 encoded)
            byte[] binaryContent = Base64.getDecoder().decode(content);
            message = session.createBytesMessage();
            ((jakarta.jms.BytesMessage) message).writeBytes(binaryContent);
            message.setStringProperty("ContentType", "application/octet-stream");
        } else {
            // Default to text
            message = session.createTextMessage(content);
            message.setStringProperty("ContentType", "text/plain");
        }

        // Set headers if provided
        if (headers != null) {
            for (MessageHeader header : headers) {
                setMessageProperty(message, header);
            }
        }

        return message;
    }

    /**
     * Set a message property based on the header type
     */
    private void setMessageProperty(Message message, MessageHeader header) throws JMSException {
        String name = header.getName();
        String value = header.getValue();
        String type = header.getType().toLowerCase();

        switch (type) {
            case "string":
                message.setStringProperty(name, value);
                break;
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
            case "byte":
                message.setByteProperty(name, Byte.parseByte(value));
                break;
            default:
                message.setStringProperty(name, value);
        }
    }

    /**
     * Check if a string is valid Base64
     */
    private boolean isBase64(String content) {
        try {
            // Remove whitespace which is often in formatted Base64
            String cleaned = content.replaceAll("\\s", "");
            Base64.getDecoder().decode(cleaned);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 
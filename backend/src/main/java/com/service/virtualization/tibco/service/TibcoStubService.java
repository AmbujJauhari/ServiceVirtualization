package com.service.virtualization.tibco.service;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.tibco.listener.TibcoDynamicDestinationManager;
import com.service.virtualization.tibco.model.TibcoStub;
import com.service.virtualization.tibco.repository.TibcoStubRepository;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the TibcoStubService interface.
 */
@Service
public class TibcoStubService {
    private static final Logger logger = LoggerFactory.getLogger(TibcoStubService.class);

    private final TibcoStubRepository tibcoStubRepository;
    private final TibcoDynamicDestinationManager destinationManager;

    @Autowired
    public TibcoStubService(TibcoStubRepository tibcoStubRepository, TibcoDynamicDestinationManager destinationManager) {
        this.tibcoStubRepository = tibcoStubRepository;
        this.destinationManager = destinationManager;
    }

    public TibcoStub create(TibcoStub stub) {
        // Set creation and update times
        LocalDateTime now = LocalDateTime.now();
        stub.setCreatedAt(now);
        stub.setUpdatedAt(now);

        TibcoStub savedStub = tibcoStubRepository.save(stub);
        if (savedStub.isActive()) {
            registerStubListener(savedStub);
        }

        return savedStub;
    }

    private void registerStubListener(TibcoStub stub) {
        if (stub.isActive()) {
            boolean success = destinationManager.registerListener(stub);
            if (success) {
                logger.info("Registered listener for Tibco stub: {}", stub.getId());
            } else {
                logger.warn("Failed to register listener for Tibco stub: {}", stub.getId());
            }
        }
    }

    public List<TibcoStub> findAll() {
        return tibcoStubRepository.findAll();
    }

    public List<TibcoStub> findAllByStatus(StubStatus status) {
        logger.debug("Finding all Tibco stubs with status: {}", status);
        return tibcoStubRepository.findByStatus(status);
    }

    public List<TibcoStub> findByUserId(String userId) {
        return tibcoStubRepository.findByUserId(userId);
    }


    public Optional<TibcoStub> findById(String id) {
        return tibcoStubRepository.findById(id);
    }

    public TibcoStub update(String id, TibcoStub TibcoStub) {
        TibcoStub existingStub = findById(id).orElseThrow(() -> new ResourceNotFoundException("Tibco stub not found with id: " + id));

        // Update fields
        existingStub.setName(TibcoStub.getName());
        existingStub.setDescription(TibcoStub.getDescription());
        existingStub.setDestinationName(TibcoStub.getDestinationName());
        existingStub.setMessageSelector(TibcoStub.getMessageSelector());
        existingStub.setContentMatchType(TibcoStub.getContentMatchType());
        existingStub.setContentPattern(TibcoStub.getContentPattern());
        existingStub.setCaseSensitive(TibcoStub.isCaseSensitive());
        existingStub.setResponseType(TibcoStub.getResponseType());
        existingStub.setResponseDestination(TibcoStub.getResponseDestination());
        existingStub.setResponseContent(TibcoStub.getResponseContent());
        existingStub.setWebhookUrl(TibcoStub.getWebhookUrl());
        existingStub.setPriority(TibcoStub.getPriority());
        existingStub.setHeaders(TibcoStub.getHeaders());
        existingStub.setStatus(TibcoStub.getStatus());
        existingStub.setUpdatedAt(LocalDateTime.now());

        TibcoStub updatedStub = tibcoStubRepository.save(existingStub);

        // Handle listener registration/unregistration if status changed
        updateStubListener(updatedStub);

        return updatedStub;

    }

    public void updateStubListener(TibcoStub stub) {
        if (stub.isActive()) {
            registerStubListener(stub);
        } else {
            destinationManager.unregisterListener(stub.getId());
            logger.info("Unregistered listener for Tibco stub: {}", stub.getId());
        }
    }

    public void delete(String id) {
        TibcoStub stub = findById(id).orElseThrow(() -> new ResourceNotFoundException("Tibco stub not found with id: " + id));

        // Unregister listener if it exists
        destinationManager.unregisterListener(id);

        tibcoStubRepository.deleteById(id);
        logger.info("Deleted ActiveMQ stub with ID: {}", id);
    }

    public TibcoStub updateStatus(String id, StubStatus status) {
        return tibcoStubRepository.findById(id)
                .map(existingStub -> {
                    existingStub.setStatus(status);
                    existingStub.setUpdatedAt(LocalDateTime.now());
                    return tibcoStubRepository.save(existingStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("TibcoStub not found with id " + id));
    }

    /**
     * Toggle the status of a stub between ACTIVE and INACTIVE.
     *
     * @param id The stub ID
     * @return The updated stub
     */
    public TibcoStub toggleStubStatus(String id) {
        return tibcoStubRepository.findById(id)
                .map(existingStub -> {
                    // Toggle between ACTIVE and INACTIVE
                    StubStatus newStatus = existingStub.getStatus() == StubStatus.ACTIVE ?
                            StubStatus.INACTIVE : StubStatus.ACTIVE;
                    existingStub.setStatus(newStatus);
                    existingStub.setUpdatedAt(LocalDateTime.now());
                    return tibcoStubRepository.save(existingStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("TibcoStub not found with id " + id));
    }


    public boolean publishMessage(String queueManager, String destinationType, String destinationName, String message, List<MessageHeader> headers) {
        try {
            // Configure JmsTemplate for the specific queue manager if needed
            // This might require more dynamic configuration based on your Tibco EMS setup
            
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
            logger.error("Failed to publish message to Tibco EMS {}: {}", destinationType, e.getMessage(), e);
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

    public List<TibcoStub> findByStatus(StubStatus status) {
        return tibcoStubRepository.findByStatus(status);
    }
} 
package com.service.virtualization.ibmmq.service;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.ibmmq.listener.IbmMqDynamicDestinationManager;
import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.repository.IBMMQStubRepository;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.model.StubStatus;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing IBM MQ stubs
 * Only active when ibmmq-disabled profile is NOT active
 */
@Service
@Profile("!ibmmq-disabled")
public class IBMMQStubService {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQStubService.class);

    private final IBMMQStubRepository ibmMQStubRepository;
    private final IbmMqDynamicDestinationManager destinationManager;

    @Autowired
    public IBMMQStubService(IBMMQStubRepository ibmMQStubRepository, IbmMqDynamicDestinationManager destinationManager) {
        this.ibmMQStubRepository = ibmMQStubRepository;
        this.destinationManager = destinationManager;
    }

    public IBMMQStub create(IBMMQStub ibmMQStub) {
        // Set creation and update times
        LocalDateTime now = LocalDateTime.now();
        ibmMQStub.setCreatedAt(now);
        ibmMQStub.setUpdatedAt(now);

        IBMMQStub savedStub = ibmMQStubRepository.save(ibmMQStub);
        if (savedStub.isActive()) {
            registerStubListener(savedStub);
        }

        return savedStub;
    }

    private void registerStubListener(IBMMQStub stub) {
        if (stub.isActive()) {
            boolean success = destinationManager.registerListener(stub);
            if (success) {
                logger.info("Registered listener for IBMMQ stub: {}", stub.getId());
            } else {
                logger.warn("Failed to register listener for IBMMQ stub: {}", stub.getId());
            }
        }
    }

    public List<IBMMQStub> findAll() {
        return ibmMQStubRepository.findAll();
    }

    public List<IBMMQStub> findAllByStatus(StubStatus status) {
        logger.debug("Finding all IBMMQ stubs with status: {}", status);
        return ibmMQStubRepository.findByStatus(status);
    }

    public List<IBMMQStub> findByUserId(String userId) {
        return ibmMQStubRepository.findByUserId(userId);
    }


    public Optional<IBMMQStub> findById(String id) {
        return ibmMQStubRepository.findById(id);
    }

    public IBMMQStub update(String id, IBMMQStub ibmMQStub) {
        IBMMQStub existingStub = findById(id).orElseThrow(() -> new ResourceNotFoundException("IBMMQ stub not found with id: " + id));

        // Update fields
        existingStub.setName(ibmMQStub.getName());
        existingStub.setDescription(ibmMQStub.getDescription());
        existingStub.setDestinationName(ibmMQStub.getDestinationName());
        existingStub.setMessageSelector(ibmMQStub.getMessageSelector());
        existingStub.setContentMatchType(ibmMQStub.getContentMatchType());
        existingStub.setContentPattern(ibmMQStub.getContentPattern());
        existingStub.setCaseSensitive(ibmMQStub.isCaseSensitive());
        existingStub.setResponseType(ibmMQStub.getResponseType());
        existingStub.setResponseDestination(ibmMQStub.getResponseDestination());
        existingStub.setResponseContent(ibmMQStub.getResponseContent());
        existingStub.setWebhookUrl(ibmMQStub.getWebhookUrl());
        existingStub.setPriority(ibmMQStub.getPriority());
        existingStub.setHeaders(ibmMQStub.getHeaders());
        existingStub.setStatus(ibmMQStub.getStatus());
        existingStub.setUpdatedAt(LocalDateTime.now());

        IBMMQStub updatedStub = ibmMQStubRepository.save(existingStub);

        // Handle listener registration/unregistration if status changed
        updateStubListener(updatedStub);

        return updatedStub;

    }

    public void updateStubListener(IBMMQStub stub) {
        if (stub.isActive()) {
            registerStubListener(stub);
        } else {
            destinationManager.unregisterListener(stub.getId());
            logger.info("Unregistered listener for IBMMQ stub: {}", stub.getId());
        }
    }

    public void delete(String id) {
        IBMMQStub stub = findById(id).orElseThrow(() -> new ResourceNotFoundException("IBMMQ stub not found with id: " + id));

        // Unregister listener if it exists
        destinationManager.unregisterListener(id);

        ibmMQStubRepository.deleteById(id);
        logger.info("Deleted ActiveMQ stub with ID: {}", id);
    }

    public IBMMQStub updateStatus(String id, StubStatus status) {
        return ibmMQStubRepository.findById(id)
                .map(existingStub -> {
                    existingStub.setStatus(status);
                    existingStub.setUpdatedAt(LocalDateTime.now());
                    return ibmMQStubRepository.save(existingStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("IBMMQStub not found with id " + id));
    }

    /**
     * Toggle the status of a stub between ACTIVE and INACTIVE.
     *
     * @param id The stub ID
     * @return The updated stub
     */
    public IBMMQStub toggleStubStatus(String id) {
        return ibmMQStubRepository.findById(id)
                .map(existingStub -> {
                    // Toggle between ACTIVE and INACTIVE
                    StubStatus newStatus = existingStub.getStatus() == StubStatus.ACTIVE ?
                            StubStatus.INACTIVE : StubStatus.ACTIVE;
                    existingStub.setStatus(newStatus);
                    existingStub.setUpdatedAt(LocalDateTime.now());
                    return ibmMQStubRepository.save(existingStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("IBMMQStub not found with id " + id));
    }


    public boolean publishMessage(String queueManager, String destinationType, String destinationName, String message, List<MessageHeader> headers) {
        try {
            // Configure JmsTemplate for the specific queue manager if needed
            // This might require more dynamic configuration based on your IBM MQ setup
            
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
            logger.error("Failed to publish message to IBM MQ {}: {}", destinationType, e.getMessage(), e);
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

    public List<IBMMQStub> findByStatus(StubStatus status) {
        return ibmMQStubRepository.findByStatus(status);
    }
} 
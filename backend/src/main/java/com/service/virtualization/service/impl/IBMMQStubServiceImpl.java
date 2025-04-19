package com.service.virtualization.service.impl;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.IBMMQStub;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.repository.IBMMQStubRepository;
import com.service.virtualization.service.IBMMQStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the IBMMQStubService interface
 */
@Service
public class IBMMQStubServiceImpl implements IBMMQStubService {

    private static final Logger logger = LoggerFactory.getLogger(IBMMQStubServiceImpl.class);
    
    private final IBMMQStubRepository ibmMQStubRepository;
    private final JmsTemplate jmsTemplate;

    @Autowired
    public IBMMQStubServiceImpl(IBMMQStubRepository ibmMQStubRepository, JmsTemplate jmsTemplate) {
        this.ibmMQStubRepository = ibmMQStubRepository;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public IBMMQStub create(IBMMQStub ibmMQStub) {
        // Set creation and update times
        LocalDateTime now = LocalDateTime.now();
        ibmMQStub.setCreatedAt(now);
        ibmMQStub.setUpdatedAt(now);
        
        return ibmMQStubRepository.save(ibmMQStub);
    }

    @Override
    public List<IBMMQStub> findAll() {
        return ibmMQStubRepository.findAll();
    }

    @Override
    public List<IBMMQStub> findByUserId(String userId) {
        return ibmMQStubRepository.findByUserId(userId);
    }

    @Override
    public List<IBMMQStub> findActiveByUserId(String userId) {
        return ibmMQStubRepository.findByUserIdAndStatus(userId, true);
    }

    @Override
    public Optional<IBMMQStub> findById(String id) {
        return ibmMQStubRepository.findById(id);
    }

    @Override
    public IBMMQStub update(String id, IBMMQStub ibmMQStub) {
        return ibmMQStubRepository.findById(id)
                .map(existingStub -> {
                    // Update fields
                    existingStub.setName(ibmMQStub.getName());
                    existingStub.setDescription(ibmMQStub.getDescription());
                    existingStub.setQueueManager(ibmMQStub.getQueueManager());
                    existingStub.setQueueName(ibmMQStub.getQueueName());
                    existingStub.setSelector(ibmMQStub.getSelector());
                    existingStub.setResponseContent(ibmMQStub.getResponseContent());
                    existingStub.setResponseType(ibmMQStub.getResponseType());
                    existingStub.setLatency(ibmMQStub.getLatency());
                    existingStub.setHeaders(ibmMQStub.getHeaders());
                    existingStub.setStatus(ibmMQStub.isStatus());
                    existingStub.setUpdatedAt(LocalDateTime.now());
                    
                    // Save and return updated stub
                    return ibmMQStubRepository.save(existingStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("IBMMQStub not found with id " + id));
    }

    @Override
    public void delete(String id) {
        ibmMQStubRepository.deleteById(id);
    }

    @Override
    public IBMMQStub updateStatus(String id, boolean status) {
        return ibmMQStubRepository.findById(id)
                .map(stub -> {
                    stub.setStatus(status);
                    stub.setUpdatedAt(LocalDateTime.now());
                    return ibmMQStubRepository.save(stub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("IBMMQStub not found with id " + id));
    }

    @Override
    public IBMMQStub addHeader(String stubId, MessageHeader header) {
        return ibmMQStubRepository.findById(stubId)
                .map(stub -> {
                    // Add or update header
                    List<MessageHeader> headers = stub.getHeaders();
                    // Remove existing header with same name if exists
                    headers = headers.stream()
                            .filter(h -> !h.getName().equals(header.getName()))
                            .collect(Collectors.toList());
                    // Add new header
                    headers.add(header);
                    stub.setHeaders(headers);
                    stub.setUpdatedAt(LocalDateTime.now());
                    return ibmMQStubRepository.save(stub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("IBMMQStub not found with id " + stubId));
    }

    @Override
    public IBMMQStub removeHeader(String stubId, String headerName) {
        return ibmMQStubRepository.findById(stubId)
                .map(stub -> {
                    // Remove header with given name
                    List<MessageHeader> headers = stub.getHeaders();
                    headers = headers.stream()
                            .filter(h -> !h.getName().equals(headerName))
                            .collect(Collectors.toList());
                    stub.setHeaders(headers);
                    stub.setUpdatedAt(LocalDateTime.now());
                    return ibmMQStubRepository.save(stub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("IBMMQStub not found with id " + stubId));
    }

    @Override
    public List<IBMMQStub> findByQueueName(String queueName) {
        return ibmMQStubRepository.findByQueueName(queueName);
    }

    @Override
    public List<IBMMQStub> findByQueueManagerAndName(String queueManager, String queueName) {
        return ibmMQStubRepository.findByQueueManagerAndQueueName(queueManager, queueName);
    }

    @Override
    public List<IBMMQStub> findActiveByQueueManagerAndName(String queueManager, String queueName) {
        return ibmMQStubRepository.findByQueueManagerAndQueueNameAndStatus(queueManager, queueName, true);
    }
    
    @Override
    public boolean publishMessage(String queueManager, String queueName, String message, List<MessageHeader> headers) {
        try {
            // Configure JmsTemplate for the specific queue manager if needed
            // This might require more dynamic configuration based on your IBM MQ setup
            
            // Send message to the specified queue
            jmsTemplate.send(queueName, session -> {
                Message jmsMessage = createMessage(session, message, headers);
                return jmsMessage;
            });
            
            logger.info("Successfully published message to queue '{}' on queue manager '{}'", queueName, queueManager);
            return true;
        } catch (Exception e) {
            logger.error("Failed to publish message to IBM MQ: {}", e.getMessage(), e);
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
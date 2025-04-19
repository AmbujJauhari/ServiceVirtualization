package com.service.virtualization.service.impl;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.ActiveMQStub;
import com.service.virtualization.model.MessageHeader;
import com.service.virtualization.repository.ActiveMQStubRepository;
import com.service.virtualization.service.ActiveMQStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the ActiveMQStubService interface
 */
@Service
public class ActiveMQStubServiceImpl implements ActiveMQStubService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQStubServiceImpl.class);
    
    private final ActiveMQStubRepository activeMQStubRepository;
    
    @Autowired
    public ActiveMQStubServiceImpl(ActiveMQStubRepository activeMQStubRepository) {
        this.activeMQStubRepository = activeMQStubRepository;
    }

    @Override
    public ActiveMQStub create(ActiveMQStub activeMQStub) {
        logger.info("Creating new ActiveMQ stub: {}", activeMQStub.getName());
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        activeMQStub.setCreatedAt(now);
        activeMQStub.setUpdatedAt(now);
        
        return activeMQStubRepository.save(activeMQStub);
    }

    @Override
    public List<ActiveMQStub> findAll() {
        logger.info("Finding all ActiveMQ stubs");
        return activeMQStubRepository.findAll();
    }

    @Override
    public List<ActiveMQStub> findByUserId(String userId) {
        logger.info("Finding ActiveMQ stubs by user ID: {}", userId);
        return activeMQStubRepository.findByUserId(userId);
    }

    @Override
    public List<ActiveMQStub> findActiveByUserId(String userId) {
        logger.info("Finding active ActiveMQ stubs by user ID: {}", userId);
        return activeMQStubRepository.findByUserIdAndStatus(userId, true);
    }

    @Override
    public Optional<ActiveMQStub> findById(String id) {
        logger.info("Finding ActiveMQ stub by ID: {}", id);
        return activeMQStubRepository.findById(id);
    }

    @Override
    public ActiveMQStub update(String id, ActiveMQStub activeMQStub) {
        logger.info("Updating ActiveMQ stub with ID: {}", id);
        
        return activeMQStubRepository.findById(id)
                .map(existingStub -> {
                    // Update fields
                    existingStub.setName(activeMQStub.getName());
                    existingStub.setDescription(activeMQStub.getDescription());
                    existingStub.setDestinationType(activeMQStub.getDestinationType());
                    existingStub.setDestinationName(activeMQStub.getDestinationName());
                    existingStub.setSelector(activeMQStub.getSelector());
                    existingStub.setResponseContent(activeMQStub.getResponseContent());
                    existingStub.setResponseType(activeMQStub.getResponseType());
                    existingStub.setLatency(activeMQStub.getLatency());
                    existingStub.setHeaders(activeMQStub.getHeaders());
                    existingStub.setStatus(activeMQStub.isStatus());
                    existingStub.setUpdatedAt(LocalDateTime.now());
                    
                    // Save and return
                    return activeMQStubRepository.save(existingStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with ID: " + id));
    }

    @Override
    public void delete(String id) {
        logger.info("Deleting ActiveMQ stub with ID: {}", id);
        
        ActiveMQStub stub = activeMQStubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with ID: " + id));
                
        activeMQStubRepository.delete(stub);
    }

    @Override
    public ActiveMQStub updateStatus(String id, boolean status) {
        logger.info("Updating status of ActiveMQ stub with ID: {} to {}", id, status);
        
        return activeMQStubRepository.findById(id)
                .map(stub -> {
                    stub.setStatus(status);
                    stub.setUpdatedAt(LocalDateTime.now());
                    return activeMQStubRepository.save(stub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with ID: " + id));
    }

    @Override
    public ActiveMQStub addHeader(String stubId, MessageHeader header) {
        logger.info("Adding header to ActiveMQ stub with ID: {}", stubId);
        
        return activeMQStubRepository.findById(stubId)
                .map(stub -> {
                    stub.addHeader(header);
                    stub.setUpdatedAt(LocalDateTime.now());
                    return activeMQStubRepository.save(stub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with ID: " + stubId));
    }

    @Override
    public ActiveMQStub removeHeader(String stubId, String headerName) {
        logger.info("Removing header '{}' from ActiveMQ stub with ID: {}", headerName, stubId);
        
        return activeMQStubRepository.findById(stubId)
                .map(stub -> {
                    stub.removeHeader(headerName);
                    stub.setUpdatedAt(LocalDateTime.now());
                    return activeMQStubRepository.save(stub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("ActiveMQ stub not found with ID: " + stubId));
    }

    @Override
    public List<ActiveMQStub> findByDestinationName(String destinationName) {
        logger.info("Finding ActiveMQ stubs by destination name: {}", destinationName);
        return activeMQStubRepository.findByDestinationName(destinationName);
    }

    @Override
    public List<ActiveMQStub> findByDestinationTypeAndName(String destinationType, String destinationName) {
        logger.info("Finding ActiveMQ stubs by destination type: {} and name: {}", destinationType, destinationName);
        return activeMQStubRepository.findByDestinationTypeAndDestinationName(destinationType, destinationName);
    }

    @Override
    public List<ActiveMQStub> findActiveByDestinationTypeAndName(String destinationType, String destinationName) {
        logger.info("Finding active ActiveMQ stubs by destination type: {} and name: {}", destinationType, destinationName);
        return activeMQStubRepository.findByDestinationTypeAndDestinationNameAndStatus(destinationType, destinationName, true);
    }

    @Override
    public boolean publishMessage(String destinationType, String destinationName, String message, List<MessageHeader> headers) {
        logger.info("Publishing message to ActiveMQ destination type: {} and name: {}", destinationType, destinationName);
        
        try {
            // Actual implementation would connect to ActiveMQ and send the message
            // This is a placeholder implementation
            logger.info("Message content: {}", message);
            logger.info("Headers: {}", headers);
            
            // In a real implementation, this would use a JmsTemplate or similar
            // to send the message to the ActiveMQ broker
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to publish message to ActiveMQ: {}", e.getMessage(), e);
            return false;
        }
    }
} 
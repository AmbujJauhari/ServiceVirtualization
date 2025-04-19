package com.service.virtualization.activemq.service;

import com.service.virtualization.activemq.listener.DynamicDestinationManager;
import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.repository.ActiveMQStubRepository;
import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    private DynamicDestinationManager destinationManager;
    
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
    public ActiveMQStub createStub(ActiveMQStub stub) {
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
        
        activeMQStubRepository.deleteById(id);
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
    private void updateStubListener(ActiveMQStub stub) {
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
} 
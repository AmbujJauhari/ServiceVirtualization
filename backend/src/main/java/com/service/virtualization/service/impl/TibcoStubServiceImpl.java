package com.service.virtualization.service.impl;

import com.service.virtualization.model.TibcoStub;
import com.service.virtualization.repository.TibcoStubRepository;
import com.service.virtualization.service.TibcoStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the TibcoStubService interface.
 */
@Service
public class TibcoStubServiceImpl implements TibcoStubService {
    private static final Logger logger = LoggerFactory.getLogger(TibcoStubServiceImpl.class);
    
    private final TibcoStubRepository tibcoStubRepository;

    public TibcoStubServiceImpl(TibcoStubRepository tibcoStubRepository) {
        this.tibcoStubRepository = tibcoStubRepository;
    }

    @Override
    public TibcoStub createStub(TibcoStub tibcoStub) {
        logger.debug("Creating TIBCO stub: {}", tibcoStub.getName());
        validateStub(tibcoStub);
        return tibcoStubRepository.save(tibcoStub);
    }

    @Override
    public TibcoStub updateStub(String id, TibcoStub tibcoStub) {
        logger.debug("Updating TIBCO stub with ID: {}", id);
        
        Optional<TibcoStub> existingStub = tibcoStubRepository.findById(id);
        if (existingStub.isEmpty()) {
            throw new IllegalArgumentException("TIBCO stub not found with ID: " + id);
        }
        
        validateStub(tibcoStub);
        tibcoStub.setId(id);
        
        // Preserve creation date
        tibcoStub.setCreatedAt(existingStub.get().getCreatedAt());
        
        return tibcoStubRepository.save(tibcoStub);
    }

    @Override
    public Optional<TibcoStub> getStubById(String id) {
        logger.debug("Getting TIBCO stub with ID: {}", id);
        return tibcoStubRepository.findById(id);
    }

    @Override
    public List<TibcoStub> getAllStubs() {
        logger.debug("Getting all TIBCO stubs");
        return tibcoStubRepository.findAll();
    }

    @Override
    public List<TibcoStub> getStubsByStatus(String status) {
        logger.debug("Getting TIBCO stubs with status: {}", status);
        return tibcoStubRepository.findByStatus(status);
    }

    @Override
    public List<TibcoStub> getStubsByUserId(String userId) {
        logger.debug("Getting TIBCO stubs for user: {}", userId);
        return tibcoStubRepository.findByUserId(userId);
    }

    @Override
    public void deleteStub(String id) {
        logger.debug("Deleting TIBCO stub with ID: {}", id);
        tibcoStubRepository.deleteById(id);
    }

    @Override
    public TibcoStub activateStub(String id) {
        logger.debug("Activating TIBCO stub with ID: {}", id);
        
        Optional<TibcoStub> stubOptional = tibcoStubRepository.findById(id);
        if (stubOptional.isEmpty()) {
            throw new IllegalArgumentException("TIBCO stub not found with ID: " + id);
        }
        
        TibcoStub stub = stubOptional.get();
        stub.setStatus("ACTIVE");
        
        return tibcoStubRepository.save(stub);
    }

    @Override
    public TibcoStub deactivateStub(String id) {
        logger.debug("Deactivating TIBCO stub with ID: {}", id);
        
        Optional<TibcoStub> stubOptional = tibcoStubRepository.findById(id);
        if (stubOptional.isEmpty()) {
            throw new IllegalArgumentException("TIBCO stub not found with ID: " + id);
        }
        
        TibcoStub stub = stubOptional.get();
        stub.setStatus("INACTIVE");
        
        return tibcoStubRepository.save(stub);
    }

    @Override
    public List<TibcoStub> findAllByStatus(String status) {
        logger.debug("Finding all TIBCO stubs with status: {}", status);
        return tibcoStubRepository.findByStatus(status);
    }
    
    /**
     * Validates a TIBCO stub.
     *
     * @param tibcoStub The TIBCO stub to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateStub(TibcoStub tibcoStub) {
        if (tibcoStub.getName() == null || tibcoStub.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("TIBCO stub name cannot be empty");
        }
        
        if (tibcoStub.getRequestDestination() == null) {
            throw new IllegalArgumentException("TIBCO stub destination cannot be null");
        }
        
        if (tibcoStub.getRequestDestination().getType() == null || tibcoStub.getRequestDestination().getType().trim().isEmpty()) {
            throw new IllegalArgumentException("TIBCO destination type cannot be empty");
        }
        
        if (tibcoStub.getRequestDestination().getName() == null || tibcoStub.getRequestDestination().getName().trim().isEmpty()) {
            throw new IllegalArgumentException("TIBCO destination name cannot be empty");
        }
        
        if (tibcoStub.getResponseType() == null || tibcoStub.getResponseType().trim().isEmpty()) {
            throw new IllegalArgumentException("TIBCO response type cannot be empty");
        }
        
        if (tibcoStub.getResponseContent() == null || tibcoStub.getResponseContent().trim().isEmpty()) {
            throw new IllegalArgumentException("TIBCO response content cannot be empty");
        }
    }
} 
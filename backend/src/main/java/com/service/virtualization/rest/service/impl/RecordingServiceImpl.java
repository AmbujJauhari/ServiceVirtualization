package com.service.virtualization.rest.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.service.virtualization.model.Recording;
import com.service.virtualization.repository.RepositoryFactory;
import com.service.virtualization.rest.service.RecordingService;

/**
 * Implementation of RecordingService that manages API recordings
 */
@Service
public class RecordingServiceImpl implements RecordingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingServiceImpl.class);
    
    private final RepositoryFactory repositoryFactory;
    
    public RecordingServiceImpl(RepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }
    
    @Override
    public Recording createRecording(Recording recording) {
        // With null check to handle the Recording record properly
        String name = recording.name();
        logger.info("Creating recording: {}", name != null ? name : "unnamed recording");
        
        // Set default values if needed but don't generate ID
        LocalDateTime recordedAt = recording.recordedAt();
        
        // Create new instance if needed
        if (recordedAt == null) {
            recording = new Recording(
                recording.id(), // Keep ID as is (null or provided)
                recording.name(),
                recording.description(),
                recording.userId(),
                recording.behindProxy(),
                recording.protocol(),
                recording.protocolData(),
                recording.createdAt(),
                recording.updatedAt(),
                recording.sessionId(),
                LocalDateTime.now(), // Default recordedAt
                recording.convertedToStub(),
                recording.convertedStubId(),
                recording.sourceIp(),
                recording.requestData(),
                recording.responseData()
            );
        }
        
        // Save to repository - database will generate ID if null
        return repositoryFactory.getRecordingRepository().save(recording);
    }
    
    @Override
    public Optional<Recording> findRecordingById(String id) {
        return repositoryFactory.getRecordingRepository().findById(id);
    }
    
    @Override
    public List<Recording> findAllRecordings() {
        return repositoryFactory.getRecordingRepository().findAll();
    }
    
    @Override
    public List<Recording> findRecordingsBySessionId(String sessionId) {
        return repositoryFactory.getRecordingRepository().findBySessionId(sessionId);
    }
    
    @Override
    public List<Recording> findRecordingsByUserId(String userId) {
        return repositoryFactory.getRecordingRepository().findByUserId(userId);
    }
    
    @Override
    public void deleteRecording(String id) {
        logger.info("Deleting recording with ID: {}", id);
        repositoryFactory.getRecordingRepository().deleteById(id);
    }
    
    /**
     * For backward compatibility - now always returns true
     * since we're using path-based configuration instead of sessions
     */
    public boolean isSessionActive(String sessionId) {
        return true;
    }
} 
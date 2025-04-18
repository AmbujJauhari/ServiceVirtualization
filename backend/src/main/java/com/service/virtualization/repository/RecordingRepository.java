package com.service.virtualization.repository;

import com.service.virtualization.model.Recording;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Recording operations
 */
public interface RecordingRepository {
    
    /**
     * Save a recording
     * 
     * @param recording the recording to save
     * @return the saved recording
     */
    Recording save(Recording recording);
    
    /**
     * Find a recording by its ID
     * 
     * @param id the ID of the recording
     * @return an Optional containing the recording, or empty if not found
     */
    Optional<Recording> findById(String id);
    
    /**
     * Find all recordings
     * 
     * @return a list of all recordings
     */
    List<Recording> findAll();
    
    /**
     * Find recordings by session ID
     * 
     * @param sessionId the session ID to filter by
     * @return a list of recordings from the specified session
     */
    List<Recording> findBySessionId(String sessionId);
    
    /**
     * Find recordings by user ID
     * 
     * @param userId the user ID to filter by
     * @return a list of recordings created by the specified user
     */
    List<Recording> findByUserId(String userId);
    
    /**
     * Find recordings by service path
     * 
     * @param path the service path to filter by
     * @return a list of recordings for the specified path
     */
    List<Recording> findByServicePath(String path);
    
    /**
     * Find recordings that have not been converted to stubs
     * 
     * @return a list of recordings that haven't been converted to stubs
     */
    List<Recording> findByConvertedToStubFalse();
    
    /**
     * Delete a recording by its ID
     * 
     * @param id the ID of the recording to delete
     */
    void deleteById(String id);
    
    /**
     * Delete a recording
     * 
     * @param recording the recording to delete
     */
    void delete(Recording recording);
    
    /**
     * Check if a recording exists by ID
     * 
     * @param id the ID to check
     * @return true if a recording with the given ID exists, false otherwise
     */
    boolean existsById(String id);
} 
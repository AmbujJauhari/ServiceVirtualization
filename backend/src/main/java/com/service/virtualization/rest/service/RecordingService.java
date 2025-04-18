package com.service.virtualization.rest.service;

import com.service.virtualization.model.Recording;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing recordings
 */
public interface RecordingService {
    
    /**
     * Create a new recording
     * 
     * @param recording the recording to create
     * @return the created recording
     */
    Recording createRecording(Recording recording);
    
    /**
     * Find a recording by ID
     * 
     * @param id the ID of the recording to find
     * @return an Optional containing the recording, or empty if not found
     */
    Optional<Recording> findRecordingById(String id);
    
    /**
     * Find all recordings
     * 
     * @return a list of all recordings
     */
    List<Recording> findAllRecordings();
    
    /**
     * Find recordings by session ID
     * 
     * @param sessionId the session ID to filter by
     * @return a list of recordings from the specified session
     */
    List<Recording> findRecordingsBySessionId(String sessionId);
    
    /**
     * Find recordings by user ID
     * 
     * @param userId the user ID to filter by
     * @return a list of recordings created by the specified user
     */
    List<Recording> findRecordingsByUserId(String userId);
    
    /**
     * Delete a recording by ID
     * 
     * @param id the ID of the recording to delete
     */
    void deleteRecording(String id);
} 
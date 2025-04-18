package com.service.virtualization.repository;

import com.service.virtualization.model.RecordingConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RecordingConfig
 */
public interface RecordingConfigRepository {
    
    /**
     * Save a recording configuration
     */
    RecordingConfig save(RecordingConfig config);
    
    /**
     * Find a recording configuration by ID
     */
    Optional<RecordingConfig> findById(String id);
    
    /**
     * Find all recording configurations
     */
    List<RecordingConfig> findAll();
    
    /**
     * Find recording configurations by user ID
     */
    List<RecordingConfig> findByUserId(String userId);
    
    /**
     * Find all active recording configurations
     */
    List<RecordingConfig> findByActiveTrue();
    
    /**
     * Delete a recording configuration by ID
     */
    void deleteById(String id);
    
    /**
     * Delete a recording configuration
     */
    void delete(RecordingConfig config);
    
    /**
     * Check if a recording configuration exists by ID
     */
    boolean existsById(String id);
} 
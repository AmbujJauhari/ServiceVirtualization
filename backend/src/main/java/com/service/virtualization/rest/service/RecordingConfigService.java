package com.service.virtualization.rest.service;

import com.service.virtualization.model.RecordingConfig;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing recording configurations
 */
public interface RecordingConfigService {
    
    /**
     * Create a new recording configuration
     */
    RecordingConfig createConfig(RecordingConfig config);
    
    /**
     * Update an existing recording configuration
     */
    RecordingConfig updateConfig(RecordingConfig config);
    
    /**
     * Find a recording configuration by ID
     */
    Optional<RecordingConfig> findConfigById(String id);
    
    /**
     * Find all recording configurations
     */
    List<RecordingConfig> findAllConfigs();
    
    /**
     * Find recording configurations by user ID
     */
    List<RecordingConfig> findConfigsByUserId(String userId);
    
    /**
     * Delete a recording configuration
     */
    void deleteConfig(String id);
    
    /**
     * Activate a recording configuration
     */
    RecordingConfig activateConfig(String id);
    
    /**
     * Deactivate a recording configuration
     */
    RecordingConfig deactivateConfig(String id);
    
    /**
     * Check if a path should be recorded based on active configurations
     */
    boolean shouldRecordPath(String path);
} 
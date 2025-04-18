package com.service.virtualization.rest.service.impl;

import com.service.virtualization.model.RecordingConfig;
import com.service.virtualization.repository.RecordingConfigRepository;
import com.service.virtualization.rest.service.RecordingConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the RecordingConfigService interface
 */
@Service
public class RecordingConfigServiceImpl implements RecordingConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingConfigServiceImpl.class);
    
    private final RecordingConfigRepository recordingConfigRepository;
    
    public RecordingConfigServiceImpl(RecordingConfigRepository recordingConfigRepository) {
        this.recordingConfigRepository = recordingConfigRepository;
    }
    
    @Override
    @Transactional
    public RecordingConfig createConfig(RecordingConfig config) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        RecordingConfig newConfig = new RecordingConfig(
            id,
            config.name(),
            config.description(),
            config.pathPattern(),
            config.active(),
            config.userId(),
            config.useHttps(),
            config.certificateData(),
            config.certificatePassword(),
            now,
            now,
            config.tags()
        );
        
        logger.info("Creating recording configuration: {}", newConfig.name());
        return recordingConfigRepository.save(newConfig);
    }
    
    @Override
    @Transactional
    public RecordingConfig updateConfig(RecordingConfig config) {
        logger.info("Updating recording configuration: {} (ID: {})", config.name(), config.id());
        
        return recordingConfigRepository.findById(config.id())
            .map(existingConfig -> {
                // Create updated config with the same ID but updated fields
                RecordingConfig updatedConfig = new RecordingConfig(
                    existingConfig.id(),
                    config.name(),
                    config.description(),
                    config.pathPattern(),
                    config.active(),
                    config.userId(),
                    config.useHttps(),
                    config.certificateData(),
                    config.certificatePassword(),
                    existingConfig.createdAt(),
                    LocalDateTime.now(),
                    config.tags()
                );
                
                return recordingConfigRepository.save(updatedConfig);
            })
            .orElseThrow(() -> {
                logger.error("Recording configuration not found: {}", config.id());
                return new IllegalArgumentException("Recording configuration not found with ID: " + config.id());
            });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<RecordingConfig> findConfigById(String id) {
        logger.debug("Finding recording configuration by ID: {}", id);
        return recordingConfigRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RecordingConfig> findAllConfigs() {
        logger.debug("Finding all recording configurations");
        return recordingConfigRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RecordingConfig> findConfigsByUserId(String userId) {
        logger.debug("Finding recording configurations for user: {}", userId);
        return recordingConfigRepository.findByUserId(userId);
    }
    
    @Override
    @Transactional
    public void deleteConfig(String id) {
        logger.info("Deleting recording configuration: {}", id);
        recordingConfigRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public RecordingConfig activateConfig(String id) {
        logger.info("Activating recording configuration: {}", id);
        
        return recordingConfigRepository.findById(id)
            .map(config -> {
                RecordingConfig activatedConfig = config.withActive(true);
                return recordingConfigRepository.save(activatedConfig);
            })
            .orElseThrow(() -> {
                logger.error("Recording configuration not found: {}", id);
                return new IllegalArgumentException("Recording configuration not found with ID: " + id);
            });
    }
    
    @Override
    @Transactional
    public RecordingConfig deactivateConfig(String id) {
        logger.info("Deactivating recording configuration: {}", id);
        
        return recordingConfigRepository.findById(id)
            .map(config -> {
                RecordingConfig deactivatedConfig = config.withActive(false);
                return recordingConfigRepository.save(deactivatedConfig);
            })
            .orElseThrow(() -> {
                logger.error("Recording configuration not found: {}", id);
                return new IllegalArgumentException("Recording configuration not found with ID: " + id);
            });
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean shouldRecordPath(String path) {
        logger.debug("Checking if path should be recorded: {}", path);
        
        // Find all active recording configurations
        List<RecordingConfig> activeConfigs = recordingConfigRepository.findByActiveTrue();
        
        // Check if any active configuration matches the path
        return activeConfigs.stream().anyMatch(config -> config.matches(path));
    }
} 
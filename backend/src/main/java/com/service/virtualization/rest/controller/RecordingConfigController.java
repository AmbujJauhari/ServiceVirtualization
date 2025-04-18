package com.service.virtualization.rest.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.service.virtualization.config.WireMockConfig;
import com.service.virtualization.model.RecordingConfig;
import com.service.virtualization.rest.service.RecordingConfigService;

/**
 * REST controller for recording configuration operations
 */
@RestController
@RequestMapping("/recording-configs")
public class RecordingConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingConfigController.class);
    
    private final RecordingConfigService recordingConfigService;
    private final WireMockConfig wireMockConfig;
    private final String certificatesDirectory = "./certificates";
    
    public RecordingConfigController(RecordingConfigService recordingConfigService, WireMockConfig wireMockConfig) {
        this.recordingConfigService = recordingConfigService;
        this.wireMockConfig = wireMockConfig;
        
        // Ensure certificates directory exists
        try {
            Files.createDirectories(Paths.get(certificatesDirectory));
        } catch (IOException e) {
            logger.error("Failed to create certificates directory", e);
        }
    }
    
    /**
     * Get all recording configs
     */
    @GetMapping
    public ResponseEntity<List<RecordingConfig>> getAllRecordingConfigs() {
        logger.debug("Getting all recording configs");
        return ResponseEntity.ok(recordingConfigService.findAllConfigs());
    }
    
    /**
     * Get recording config by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordingConfig> getRecordingConfigById(@PathVariable String id) {
        logger.debug("Getting recording config with ID: {}", id);
        return recordingConfigService.findConfigById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording config not found with ID: " + id));
    }
    
    /**
     * Create a new recording config
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordingConfig> createRecordingConfig(
            @RequestPart("config") RecordingConfig recordingConfig,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate,
            @RequestParam(value = "certificatePassword", required = false) String certificatePassword) {
        
        // Save certificate if provided
        if (certificate != null && !certificate.isEmpty()) {
            try {
                byte[] certificateData = certificate.getBytes();
                recordingConfig = updateConfigWithCertificateData(recordingConfig, certificateData, certificatePassword);
            } catch (IOException e) {
                logger.error("Failed to process certificate", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Failed to process certificate: " + e.getMessage());
            }
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordingConfigService.createConfig(recordingConfig));
    }
    
    /**
     * Update an existing recording config
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecordingConfig> updateRecordingConfig(@PathVariable String id, @RequestBody RecordingConfig recordingConfig) {
        logger.debug("Updating recording config with ID: {}", id);
        
        // Ensure ID matches
        if (!id.equals(recordingConfig.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path must match ID in body");
        }
        
        return ResponseEntity.ok(recordingConfigService.updateConfig(recordingConfig));
    }
    
    /**
     * Delete a recording config
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecordingConfig(@PathVariable String id) {
        logger.debug("Deleting recording config with ID: {}", id);
        recordingConfigService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update active status of a recording config
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<RecordingConfig> updateActiveStatus(@PathVariable String id, @RequestParam boolean active) {
        logger.debug("Updating active status of recording config {} to {}", id, active);
        
        return recordingConfigService.findConfigById(id)
                .map(config -> {
                    RecordingConfig updatedConfig = config.withActive(active);
                    return ResponseEntity.ok(recordingConfigService.updateConfig(updatedConfig));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording config not found with ID: " + id));
    }
    
    /**
     * Upload certificate for HTTPS recording
     */
    @PostMapping(value = "/{id}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordingConfig> uploadCertificate(
            @PathVariable String id,
            @RequestParam("certificate") MultipartFile certificate,
            @RequestParam("password") String password,
            @RequestParam(value = "useHttps", defaultValue = "true") boolean useHttps) {
        
        logger.debug("Uploading certificate for recording config with ID: {}", id);
        
        // Find the config
        RecordingConfig config = recordingConfigService.findConfigById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording config not found with ID: " + id));
        
        try {
            // Read certificate bytes instead of saving to filesystem
            byte[] certificateData = certificate.getBytes();
            
            // Update the recording config with HTTPS settings
            RecordingConfig updatedConfig = config.withHttpsSettings(
                    useHttps,
                    certificateData,
                    password
            );
            
            return ResponseEntity.ok(recordingConfigService.updateConfig(updatedConfig));
        } catch (IOException e) {
            logger.error("Failed to process certificate", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process certificate: " + e.getMessage());
        }
    }
    
    /**
     * Update HTTPS settings for a recording config
     */
    @PatchMapping("/{id}/https-settings")
    public ResponseEntity<RecordingConfig> updateHttpsSettings(
            @PathVariable String id,
            @RequestParam("useHttps") boolean useHttps,
            @RequestParam(value = "certificatePassword", required = false) String certificatePassword) {
        
        logger.debug("Updating HTTPS settings for recording config with ID: {}", id);
        
        return recordingConfigService.findConfigById(id)
                .map(config -> {
                    RecordingConfig updatedConfig = config.withHttpsSettings(
                            useHttps,
                            config.certificateData(), // Keep existing data
                            certificatePassword != null ? certificatePassword : config.certificatePassword() // Update password if provided
                    );
                    return ResponseEntity.ok(recordingConfigService.updateConfig(updatedConfig));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording config not found with ID: " + id));
    }
    
    private RecordingConfig updateConfigWithCertificateData(RecordingConfig config, byte[] certificateData, String certificatePassword) {
        return config.withHttpsSettings(
                true,  // Set useHttps to true since we're adding certificate data
                certificateData,
                certificatePassword
        );
    }

    /**
     * Starts a recording session for the given target URL
     * @param targetUrl The URL to record from
     * @return The recording session ID
     */
    @SuppressWarnings("unused")
    private String startRecording(String targetUrl) {
        try {
            // Generate unique recording ID
            String recordingId = UUID.randomUUID().toString();
            
            // Create URL for target
            URL targetUrlObj = new URI(targetUrl).toURL();
            
            // Check if this path should use HTTPS from a recording config
            String path = targetUrlObj.getPath();
            
            // Find a matching recording config with HTTPS settings
            Optional<RecordingConfig> configOpt = recordingConfigService.findAllConfigs().stream()
                .filter(c -> c.active() && c.matches(path) && c.useHttps() && 
                       c.certificateData() != null && c.certificateData().length > 0 &&
                       c.certificatePassword() != null)
                .findFirst();
            
            // Start recording using WireMock's API
            wireMockConfig.wireMockServer().startRecording(targetUrlObj.toString());
            
            // Set recording options
            wireMockConfig.wireMockServer().setGlobalFixedDelay(0); // No artificial delay
            
            // Apply HTTPS settings if available
            if (configOpt.isPresent()) {
                RecordingConfig config = configOpt.get();
                logger.info("Using HTTPS settings from config: {}", config.name());
                
                // Configure HTTPS with certificate data from database
                wireMockConfig.configureHttps(config.certificateData(), config.certificatePassword());
            }
            
            return recordingId;
        } catch (Exception e) {
            logger.error("Failed to start WireMock recording", e);
            return null;
        }
    }
} 
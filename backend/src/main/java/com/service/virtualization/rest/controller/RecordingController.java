package com.service.virtualization.rest.controller;

import com.service.virtualization.model.Recording;
import com.service.virtualization.rest.service.RecordingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for recording operations
 */
@RestController
@RequestMapping("/recordings")
public class RecordingController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingController.class);
    
    private final RecordingService recordingService;
    
    public RecordingController(RecordingService recordingService) {
        this.recordingService = recordingService;
    }
    
    /**
     * Get all recordings
     */
    @GetMapping
    public ResponseEntity<List<Recording>> getAllRecordings() {
        logger.debug("Getting all recordings");
        return ResponseEntity.ok(recordingService.findAllRecordings());
    }
    
    /**
     * Get recording by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Recording> getRecordingById(@PathVariable String id) {
        logger.debug("Getting recording with ID: {}", id);
        return recordingService.findRecordingById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found with ID: " + id));
    }
    
    /**
     * Get recordings by session ID
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Recording>> getRecordingsBySessionId(@PathVariable String sessionId) {
        logger.debug("Getting recordings for session: {}", sessionId);
        return ResponseEntity.ok(recordingService.findRecordingsBySessionId(sessionId));
    }
    
    /**
     * Get recordings by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Recording>> getRecordingsByUserId(@PathVariable String userId) {
        logger.debug("Getting recordings for user: {}", userId);
        return ResponseEntity.ok(recordingService.findRecordingsByUserId(userId));
    }
    
    /**
     * Create a new recording
     */
    @PostMapping
    public ResponseEntity<Recording> createRecording(@RequestBody Recording recording) {
        logger.debug("Creating recording: {}", recording.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(recordingService.createRecording(recording));
    }
    
    /**
     * Delete a recording
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecording(@PathVariable String id) {
        logger.debug("Deleting recording with ID: {}", id);
        recordingService.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }
} 
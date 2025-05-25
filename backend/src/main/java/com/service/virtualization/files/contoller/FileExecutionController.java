package com.service.virtualization.files.contoller;

import com.service.virtualization.files.service.FileExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for file execution operations
 */
@RestController
@RequestMapping("/api/file/execution")
public class FileExecutionController {
    private static final Logger logger = LoggerFactory.getLogger(FileExecutionController.class);
    
    private final FileExecutionService fileExecutionService;
    
    @Autowired
    public FileExecutionController(FileExecutionService fileExecutionService) {
        this.fileExecutionService = fileExecutionService;
    }
    
    /**
     * Manually trigger execution of a file stub
     * 
     * @param stubId the file stub ID
     * @return the response indicating success or failure
     */
    @PostMapping("/{stubId}")
    public ResponseEntity<String> executeFileStub(@PathVariable String stubId) {
        logger.info("Received request to execute file stub: {}", stubId);
        
        boolean success = fileExecutionService.executeFileStub(stubId);
        
        if (success) {
            return ResponseEntity.ok("File stub execution successful");
        } else {
            return ResponseEntity.badRequest().body("File stub execution failed");
        }
    }
} 
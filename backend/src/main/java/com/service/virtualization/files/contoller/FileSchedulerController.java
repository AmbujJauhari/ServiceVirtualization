package com.service.virtualization.files.contoller;

import com.service.virtualization.dto.DtoConverter;
import com.service.virtualization.files.model.FileStub;
import com.service.virtualization.files.dto.FileStubDTO;
import com.service.virtualization.files.scheduler.DynamicTaskScheduler;
import com.service.virtualization.files.service.FileExecutionService;
import com.service.virtualization.files.service.FileStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing scheduled file tasks
 */
@RestController
@RequestMapping("/api/file/scheduler")
public class FileSchedulerController {
    private static final Logger logger = LoggerFactory.getLogger(FileSchedulerController.class);
    
    private final FileStubService fileStubService;
    private final DynamicTaskScheduler taskScheduler;
    private final FileExecutionService executionService;
    
    @Autowired
    public FileSchedulerController(FileStubService fileStubService, DynamicTaskScheduler taskScheduler, 
                                   FileExecutionService executionService) {
        this.fileStubService = fileStubService;
        this.taskScheduler = taskScheduler;
        this.executionService = executionService;
    }
    
    /**
     * DTO for scheduler information response
     */
    public record SchedulerInfoResponseDTO(
        int scheduledTaskCount,
        int activeStubsWithCronExpression,
        List<ScheduledStubDTO> scheduledStubs
    ) {}
    
    /**
     * DTO for scheduled stub information
     */
    public record ScheduledStubDTO(
        String id,
        String name,
        String cronExpression
    ) {}
    
    /**
     * Get information about scheduled tasks
     * 
     * @return information about scheduled tasks
     */
    @GetMapping("/info")
    public ResponseEntity<SchedulerInfoResponseDTO> getSchedulerInfo() {
        logger.info("Getting scheduler information");
        
        // Get active stubs with cron expressions
        List<FileStub> activeStubs = fileStubService.findActiveWithCronExpression();
        
        // Create list of scheduled stub info using DTOs
        List<ScheduledStubDTO> scheduledStubs = activeStubs.stream()
                .filter(stub -> taskScheduler.hasScheduledTask(stub.id()))
                .map(stub -> {
                    FileStubDTO dto = DtoConverter.fromFileStub(stub);
                    return new ScheduledStubDTO(dto.id(), dto.name(), dto.cronExpression());
                })
                .collect(Collectors.toList());
        
        SchedulerInfoResponseDTO response = new SchedulerInfoResponseDTO(
            taskScheduler.getScheduledTaskCount(),
            activeStubs.size(),
            scheduledStubs
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get list of active scheduled stubs
     * 
     * @return list of scheduled stubs
     */
    @GetMapping
    public ResponseEntity<List<FileStubDTO>> getScheduledStubs() {
        logger.info("Getting list of scheduled stubs");
        
        List<FileStubDTO> scheduledStubs = fileStubService.findActiveWithCronExpression().stream()
                .filter(stub -> taskScheduler.hasScheduledTask(stub.id()))
                .map(DtoConverter::fromFileStub)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(scheduledStubs);
    }
    
    /**
     * Manually trigger a scheduled task
     * 
     * @param stubId the file stub ID
     * @return the response indicating success or failure
     */
    @PostMapping("/execute/{stubId}")
    public ResponseEntity<String> executeScheduledTask(@PathVariable String stubId) {
        logger.info("Manually triggering scheduled task for file stub: {}", stubId);
        
        return fileStubService.findById(stubId)
                .map(stub -> {
                    if (stub.cronExpression() == null || stub.cronExpression().isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body("File stub does not have a cron expression");
                    }
                    
                    // Execute the task
                    boolean success = executionService.executeFileStub(stubId);
                    if (success) {
                        return ResponseEntity.ok("Successfully executed file stub: " + stubId);
                    } else {
                        return ResponseEntity.badRequest()
                                .body("Failed to execute file stub: " + stubId);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Schedule or reschedule a task
     * 
     * @param stubId the file stub ID
     * @return the response indicating success or failure
     */
    @PostMapping("/schedule/{stubId}")
    public ResponseEntity<String> scheduleTask(@PathVariable String stubId) {
        logger.info("Scheduling task for file stub: {}", stubId);
        
        return fileStubService.findById(stubId)
                .map(stub -> {
                    if (stub.cronExpression() == null || stub.cronExpression().isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body("File stub does not have a cron expression");
                    }
                    
                    boolean scheduled = taskScheduler.scheduleTask(stub);
                    if (scheduled) {
                        return ResponseEntity.ok("Scheduled task for file stub: " + stubId);
                    } else {
                        return ResponseEntity.badRequest()
                                .body("Failed to schedule task for file stub: " + stubId);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Cancel a scheduled task
     * 
     * @param stubId the file stub ID
     * @return the response indicating success or failure
     */
    @PostMapping("/cancel/{stubId}")
    public ResponseEntity<String> cancelTask(@PathVariable String stubId) {
        logger.info("Cancelling scheduled task for file stub: {}", stubId);
        
        boolean cancelled = taskScheduler.cancelTask(stubId);
        if (cancelled) {
            return ResponseEntity.ok("Cancelled scheduled task for file stub: " + stubId);
        } else {
            return ResponseEntity.badRequest()
                    .body("No scheduled task found for file stub: " + stubId);
        }
    }
} 
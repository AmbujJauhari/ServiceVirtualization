package com.service.virtualization.files.scheduler;

import com.service.virtualization.files.model.FileStub;
import com.service.virtualization.files.service.FileStubService;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for initializing and managing file stub scheduled tasks
 */
@Service
@ConditionalOnProperty(name = "virtualization.files.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class FileSchedulerInitializer {
    private static final Logger logger = LoggerFactory.getLogger(FileSchedulerInitializer.class);
    
    private final FileStubService fileStubService;
    private final DynamicTaskScheduler taskScheduler;
    
    @Autowired
    public FileSchedulerInitializer(FileStubService fileStubService, DynamicTaskScheduler taskScheduler) {
        this.fileStubService = fileStubService;
        this.taskScheduler = taskScheduler;
    }
    
    /**
     * Initialize scheduled tasks for existing file stubs when the application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeScheduledTasks() {
        logger.info("Initializing scheduled tasks for existing file stubs");
        
        List<FileStub> activeStubs = fileStubService.findActiveWithCronExpression();
        int scheduledCount = 0;
        
        for (FileStub stub : activeStubs) {
            if (taskScheduler.scheduleTask(stub)) {
                scheduledCount++;
            }
        }
        
        logger.info("Initialized {} scheduled tasks out of {} active stubs with cron expressions", 
                scheduledCount, activeStubs.size());
    }
    
    /**
     * Handle file stub creation - schedule the task if it has a cron expression and is active
     * 
     * @param fileStub the created file stub
     */
    public void handleFileStubCreated(FileStub fileStub) {
        logger.debug("Handling file stub creation: {}", fileStub.id());
        
        if (fileStub.cronExpression() != null && !fileStub.cronExpression().isEmpty() && 
                fileStub.status() == StubStatus.ACTIVE) {
            taskScheduler.scheduleTask(fileStub);
        }
    }
    
    /**
     * Handle file stub update - reschedule the task if needed
     * 
     * @param fileStub the updated file stub
     */
    public void handleFileStubUpdated(FileStub fileStub) {
        logger.debug("Handling file stub update: {}", fileStub.id());
        
        // Cancel any existing task
        taskScheduler.cancelTask(fileStub.id());
        
        // Reschedule if it has a cron expression and is active
        if (fileStub.cronExpression() != null && !fileStub.cronExpression().isEmpty() && 
                fileStub.status() == StubStatus.ACTIVE) {
            taskScheduler.scheduleTask(fileStub);
        }
    }
    
    /**
     * Handle file stub status update - enable/disable scheduling based on status
     * 
     * @param fileStub the file stub with updated status
     */
    public void handleFileStubStatusUpdated(FileStub fileStub) {
        logger.debug("Handling file stub status update: {}, status: {}", fileStub.id(), fileStub.status());
        
        if (fileStub.status() == StubStatus.ACTIVE) {
            // If active and has cron expression, schedule it
            if (fileStub.cronExpression() != null && !fileStub.cronExpression().isEmpty()) {
                taskScheduler.scheduleTask(fileStub);
            }
        } else {
            // If not active, cancel any scheduled task
            taskScheduler.cancelTask(fileStub.id());
        }
    }
    
    /**
     * Handle file stub cron expression update - reschedule with new expression
     * 
     * @param fileStub the file stub with updated cron expression
     */
    public void handleFileStubCronExpressionUpdated(FileStub fileStub) {
        logger.debug("Handling file stub cron expression update: {}, expression: {}", 
                fileStub.id(), fileStub.cronExpression());
        
        // Cancel any existing task
        taskScheduler.cancelTask(fileStub.id());
        
        // Reschedule if it has a cron expression and is active
        if (fileStub.cronExpression() != null && !fileStub.cronExpression().isEmpty() && 
                fileStub.status() == StubStatus.ACTIVE) {
            taskScheduler.scheduleTask(fileStub);
        }
    }
    
    /**
     * Handle file stub deletion - cancel any scheduled task
     * 
     * @param stubId the ID of the deleted file stub
     */
    public void handleFileStubDeleted(String stubId) {
        logger.debug("Handling file stub deletion: {}", stubId);
        taskScheduler.cancelTask(stubId);
    }
} 
package com.service.virtualization.files.scheduler;

import com.service.virtualization.files.model.FileStub;
import com.service.virtualization.files.service.FileExecutionService;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service for dynamically scheduling file stub tasks based on cron expressions
 */
@Service
@ConditionalOnProperty(name = "virtualization.files.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DynamicTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DynamicTaskScheduler.class);
    
    private final TaskScheduler taskScheduler;
    private final FileExecutionService fileExecutionService;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    @Autowired
    public DynamicTaskScheduler(FileExecutionService fileExecutionService) {
        this.fileExecutionService = fileExecutionService;
        this.taskScheduler = createTaskScheduler();
    }
    
    @Bean
    public TaskScheduler fileTaskScheduler() {
        return createTaskScheduler();
    }
    
    private ThreadPoolTaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("file-scheduler-");
        scheduler.setErrorHandler(t -> logger.error("Error in scheduled task", t));
        scheduler.initialize();
        return scheduler;
    }
    
    /**
     * Schedule a file stub task
     * 
     * @param fileStub the file stub to schedule
     * @return true if scheduled successfully, false otherwise
     */
    public boolean scheduleTask(FileStub fileStub) {
        if (fileStub == null || fileStub.id() == null) {
            logger.warn("Cannot schedule null file stub or stub without ID");
            return false;
        }
        
        // Cancel any existing task for this stub
        cancelTask(fileStub.id());
        
        // Only schedule if the stub has a cron expression and is active
        if (fileStub.cronExpression() == null || fileStub.cronExpression().isEmpty()) {
            logger.info("File stub {} has no cron expression, not scheduling", fileStub.id());
            return false;
        }
        
        if (fileStub.status() != StubStatus.ACTIVE) {
            logger.info("File stub {} is not active, not scheduling", fileStub.id());
            return false;
        }
        
        try {
            logger.info("Scheduling file stub: {} with cron expression: {}", fileStub.id(), fileStub.cronExpression());
            
            // Create the task runnable
            Runnable task = () -> {
                try {
                    logger.info("Executing scheduled task for file stub: {}", fileStub.id());
                    fileExecutionService.executeFileStub(fileStub.id());
                } catch (Exception e) {
                    logger.error("Error executing scheduled task for file stub: {}", fileStub.id(), e);
                }
            };
            
            // Schedule the task with Spring's TaskScheduler
            CronTrigger trigger = new CronTrigger(fileStub.cronExpression());
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(task, trigger);
            
            // Store the reference to the scheduled task
            scheduledTasks.put(fileStub.id(), scheduledTask);
            logger.info("Successfully scheduled file stub: {}", fileStub.id());
            
            return true;
        } catch (Exception e) {
            logger.error("Error scheduling file stub: {}", fileStub.id(), e);
            return false;
        }
    }
    
    /**
     * Cancel a scheduled task for a file stub
     * 
     * @param stubId the ID of the file stub
     * @return true if cancelled successfully, false if the task was not found
     */
    public boolean cancelTask(String stubId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(stubId);
        if (scheduledTask != null) {
            boolean cancelled = scheduledTask.cancel(false);
            logger.info("Cancelled scheduled task for file stub: {}, result: {}", stubId, cancelled);
            return cancelled;
        }
        return false;
    }
    
    /**
     * Check if a file stub has a scheduled task
     * 
     * @param stubId the ID of the file stub
     * @return true if the stub has a scheduled task, false otherwise
     */
    public boolean hasScheduledTask(String stubId) {
        return scheduledTasks.containsKey(stubId);
    }
    
    /**
     * Get the number of currently scheduled tasks
     * 
     * @return the number of scheduled tasks
     */
    public int getScheduledTaskCount() {
        return scheduledTasks.size();
    }
    
    /**
     * Clean up resources when the service is destroyed
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down dynamic task scheduler");
        
        // Cancel all scheduled tasks
        scheduledTasks.forEach((id, task) -> {
            logger.info("Cancelling scheduled task for file stub: {}", id);
            task.cancel(false);
        });
        scheduledTasks.clear();
    }
} 
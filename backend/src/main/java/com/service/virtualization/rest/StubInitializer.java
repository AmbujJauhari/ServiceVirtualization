package com.service.virtualization.rest;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.rest.model.RestStub;
import com.service.virtualization.rest.service.RestStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes all active stubs from the database when the application starts
 */
@Component
public class StubInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(StubInitializer.class);
    
    private final RestStubService restStubService;
    
    public StubInitializer(RestStubService restStubService) {
        this.restStubService = restStubService;
    }
    
    @Override
    @Transactional(readOnly = true)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Initializing stubs from database...");
        
        try {
            // Get all active stubs from database
            List<RestStub> activeStubs = restStubService.findAllStubs().stream()
                .filter(stub -> stub.status() == StubStatus.ACTIVE)
                .toList();
            
            logger.info("Found {} active stubs to initialize", activeStubs.size());
            
            // Use AtomicInteger to track progress
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // Initialize each active stub in parallel
            activeStubs.parallelStream().forEach(stub -> {
                try {
                    restStubService.updateStub(stub);
                    logger.debug("Initialized stub: {} (ID: {})", stub.name(), stub.id());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("Failed to initialize stub: {} (ID: {}): {}", 
                                stub.name(), stub.id(), e.getMessage());
                    errorCount.incrementAndGet();
                }
            });
            
            logger.info("Stub initialization complete. Success: {}, Failed: {}", 
                       successCount.get(), errorCount.get());
            
            if (errorCount.get() > 0) {
                logger.warn("Some stubs failed to initialize. Check the logs for details.");
            }
        } catch (Exception e) {
            logger.error("Critical error initializing stubs", e);
            // Don't rethrow - we want the application to continue starting up
        }
    }
}
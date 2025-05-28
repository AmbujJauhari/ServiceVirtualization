package com.service.virtualization.soap;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.soap.service.SoapStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes all active SOAP stubs from the database when the application starts
 */
@Component
public class SoapStubInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(SoapStubInitializer.class);
    
    private final SoapStubService soapStubService;
    
    public SoapStubInitializer(SoapStubService soapStubService) {
        this.soapStubService = soapStubService;
    }
    
    @Override
    @Transactional(readOnly = true)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("Initializing SOAP stubs from database...");
        
        try {
            // Get all active stubs from database
            List<SoapStub> activeStubs = soapStubService.findAllStubs().stream()
                .filter(stub -> stub.status() == StubStatus.ACTIVE)
                .toList();
            
            logger.info("Found {} active SOAP stubs to initialize", activeStubs.size());
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // Initialize each active stub in parallel
            activeStubs.parallelStream().forEach(stub -> {
                try {
                    soapStubService.updateStub(stub);
                    logger.debug("Initialized SOAP stub: {} (ID: {})", stub.name(), stub.id());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("Failed to initialize SOAP stub: {} (ID: {}): {}", 
                                stub.name(), stub.id(), e.getMessage());
                    errorCount.incrementAndGet();
                }
            });
            
            logger.info("SOAP stub initialization complete. Success: {}, Failed: {}", 
                       successCount.get(), errorCount.get());
            
            if (errorCount.get() > 0) {
                logger.warn("Some SOAP stubs failed to initialize. Check the logs for details.");
            }
        } catch (Exception e) {
            logger.error("Critical error initializing SOAP stubs", e);
            // Don't rethrow - we want the application to continue starting up
        }
    }
} 
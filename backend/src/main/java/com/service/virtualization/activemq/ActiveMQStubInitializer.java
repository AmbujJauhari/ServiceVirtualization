package com.service.virtualization.activemq;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.service.ActiveMQStubService;
import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.service.IBMMQStubService;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Initializer for IBM MQ stubs that runs on application startup.
 * Ensures all active IBM MQ stubs are properly initialized and ready to process messages.
 */
@Component
public class ActiveMQStubInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQStubInitializer.class);

    private final ActiveMQStubService activeMQStubService;

    @Autowired
    public ActiveMQStubInitializer(ActiveMQStubService activeMQStubService) {
        this.activeMQStubService = activeMQStubService;
    }

    /**
     * Initialize all active IBM MQ stubs when the application is ready.
     * This ensures that all stubs are properly configured and ready to handle messages.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeActiveStubs() {
        logger.info("🚀 Starting IBM MQ stub initialization...");

        try {
            // Find all active stubs
            List<ActiveMQStub> activeStubs = activeMQStubService.getActiveStubs();

            if (activeStubs.isEmpty()) {
                logger.info("📭 No active IBM MQ stubs found to initialize");
                return;
            }

            logger.info("🔧 Found {} active IBM MQ stubs to initialize", activeStubs.size());

            // Initialize stubs in parallel for better performance
            CompletableFuture<Void> initializationFuture = CompletableFuture.allOf(
                    activeStubs.stream()
                            .map(this::initializeStub)
                            .toArray(CompletableFuture[]::new)
            );

            // Wait for all initializations to complete
            initializationFuture.join();

            logger.info("✅ IBM MQ stub initialization completed successfully! {} stubs are ready", activeStubs.size());

        } catch (Exception e) {
            logger.error("💥 Error during IBM MQ stub initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize a single IBM MQ stub.
     *
     * @param stub The stub to initialize
     * @return CompletableFuture for async processing
     */
    private CompletableFuture<Void> initializeStub(ActiveMQStub stub) {
        return CompletableFuture.runAsync(() -> {
            try {

                // Validate stub configuration
                validateStubConfiguration(stub);

                // Initialize queue connections if needed
                initializeQueueConnections(stub);

                logger.debug("✅ Successfully initialized IBM MQ stub: {}", stub.getName());

            } catch (Exception e) {
                logger.error("❌ Failed to initialize IBM MQ stub '{}': {}", stub.getName(), e.getMessage(), e);
            }
        });
    }

    /**
     * Validate the configuration of an IBM MQ stub.
     *
     * @param stub The stub to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateStubConfiguration(ActiveMQStub stub) {
        if (stub.getName() == null || stub.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stub name cannot be empty");
        }


        // Validate content matching configuration
        if (stub.getContentMatchType() != null ) {
            if (stub.getContentPattern() == null || stub.getContentPattern().trim().isEmpty()) {
                logger.warn("⚠️ Content matching type is set but pattern is empty for stub: {}", stub.getName());
            }
        }

        // Validate response configuration
        if (stub.getResponseContent() != null && !stub.getResponseContent().trim().isEmpty()) {
            if (stub.getResponseDestination() == null || stub.getResponseDestination().trim().isEmpty()) {
                logger.debug("📝 Response destination not specified for stub '{}', will use JMSReplyTo", stub.getName());
            }
        }

        logger.debug("✅ Configuration validation passed for stub: {}", stub.getName());
    }

    /**
     * Initialize queue connections for the stub.
     * This is a placeholder for any queue-specific initialization logic.
     *
     * @param stub The stub to initialize connections for
     */
    private void initializeQueueConnections(ActiveMQStub stub) {
        try {
            // In a real implementation, you might:
            // 1. Verify queue exists
            // 2. Set up specific listeners for the queue
            // 3. Configure queue-specific settings
            // 4. Test connectivity
            activeMQStubService.updateStubListener(stub);

        } catch (Exception e) {
            logger.warn("⚠️ Could not fully initialize queue connections for stub '{}': {}",
                    stub.getName(), e.getMessage());
            // Don't fail the entire initialization for connection issues
        }
    }
} 
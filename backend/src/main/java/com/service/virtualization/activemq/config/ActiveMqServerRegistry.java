package com.service.virtualization.activemq.config;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.service.ActiveMQStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Registry for multiple ActiveMQ server configurations.
 * Discovers server configurations from environment properties at application startup
 * and initializes all active stubs.
 * <p>
 * Server properties should follow the pattern:
 * {@code activemq.registry.<serverName>.<property>}
 * <p>
 * Example:
 * <pre>
 * ACTIVEMQ_REGISTRY_SERVERA_BROKER_URL=tcp://activemq-servera.company.com:61616
 * ACTIVEMQ_REGISTRY_SERVERA_USERNAME=admin
 * ACTIVEMQ_REGISTRY_SERVERA_PASSWORD=secret123
 * </pre>
 */
@Component
@Profile("!activemq-disabled")
public class ActiveMqServerRegistry {

    private static final Logger log = LoggerFactory.getLogger(ActiveMqServerRegistry.class);
    private static final String REGISTRY_PREFIX = "activemq.registry.";
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("^activemq\\.registry\\.([^.]+)\\..*");

    private final Environment environment;
    private final ActiveMQStubService activeMQStubService;
    private final Map<String, ActiveMqServerConfig> serverConfigs = new ConcurrentHashMap<>();

    @Autowired
    public ActiveMqServerRegistry(Environment environment, @Lazy ActiveMQStubService activeMQStubService) {
        this.environment = environment;
        this.activeMQStubService = activeMQStubService;
    }

    /**
     * Eagerly loads server configurations during bean construction so that
     * connection factory registries that run on {@code ApplicationReadyEvent} see a populated registry.
     */
    @PostConstruct
    public void postConstruct() {
        initializeServerRegistry();
    }

    /**
     * After the whole application context is ready, initialize active stubs.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        initializeActiveStubs();
    }

    /**
     * Initialize the multi-server registry.
     */
    private void initializeServerRegistry() {
        log.info("Initializing ActiveMQ Server Registry...");

        Set<String> serverNames = discoverServerNames();

        if (serverNames.isEmpty()) {
            log.info("No ActiveMQ multi-server configurations found (using legacy single-server mode if configured).");
            return;
        }

        log.info(" Discovered {} ActiveMQ server(s): {}", serverNames.size(), serverNames);

        for (String serverName : serverNames) {
            try {
                ActiveMqServerConfig config = loadServerConfig(serverName);
                config.validate();
                serverConfigs.put(serverName, config);
                log.info("Registered ActiveMQ server '{}': {}", serverName, config.getBrokerUrl());
            } catch (IllegalStateException e) {
                log.error("Failed to register ActiveMQ server '{}': {}", serverName, e.getMessage());
                throw new IllegalStateException("Invalid ActiveMQ server configuration for '" + serverName + "': " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during ActiveMQ server '{}' initialization: {}", serverName, e.getMessage(), e);
                throw new IllegalStateException("Unexpected error during ActiveMQ server '" + serverName + "' initialization: " + e.getMessage(), e);
            }
        }

        log.info("🎉 ActiveMQ Server Registry initialized successfully with {} server(s)", serverConfigs.size());
    }

    /**
     * Initialize all active ActiveMQ stubs when the application is ready.
     * This ensures that all stubs are properly configured and ready to handle messages.
     */
    private void initializeActiveStubs() {
        log.info("Starting ActiveMQ stub initialization...");

        try {
            // Find all active stubs
            List<ActiveMQStub> activeStubs = activeMQStubService.getActiveStubs();

            if (activeStubs.isEmpty()) {
                log.info("No active ActiveMQ stubs found to initialize");
                return;
            }

            log.info("Found {} active ActiveMQ stubs to initialize", activeStubs.size());

            // Initialize stubs sequentially (connection setup is not expensive)
            int successCount = 0;
            for (ActiveMQStub stub : activeStubs) {
                try {
                    initializeStub(stub);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to initialize ActiveMQ stub '{}': {}", stub.getName(), e.getMessage(), e);
                }
            }

            log.info(" ActiveMQ stub initialization completed! {}/{} stubs initialized successfully",
                     successCount, activeStubs.size());

        } catch (Exception e) {
            log.error("Error during ActiveMQ stub initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize a single ActiveMQ stub.
     *
     * @param stub The stub to initialize
     */
    private void initializeStub(ActiveMQStub stub) {
        // Validate stub configuration
        validateStubConfiguration(stub);

        // Initialize queue connections
        initializeQueueConnections(stub);

        log.debug("Successfully initialized ActiveMQ stub: {}", stub.getName());
    }

    /**
     * Validate the configuration of an ActiveMQ stub.
     *
     * @param stub The stub to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateStubConfiguration(ActiveMQStub stub) {
        if (stub.getName() == null || stub.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stub name cannot be empty");
        }

        // Validate content matching configuration
        if (stub.getContentMatchType() != null) {
            if (stub.getContentPattern() == null || stub.getContentPattern().trim().isEmpty()) {
                log.warn("Content matching type is set but pattern is empty for stub: {}", stub.getName());
            }
        }

        // Validate response configuration
        if (stub.getResponseContent() != null && !stub.getResponseContent().trim().isEmpty()) {
            if (stub.getResponseDestination() == null || stub.getResponseDestination().trim().isEmpty()) {
                log.debug("Response destination not specified for stub '{}', will use JMSReplyTo", stub.getName());
            }
        }

        log.debug("Configuration validation passed for stub: {}", stub.getName());
    }

    /**
     * Initialize queue connections for the stub.
     *
     * @param stub The stub to initialize connections for
     */
    private void initializeQueueConnections(ActiveMQStub stub) {
        try {
            // Update the stub listener to start listening on the configured destination
            activeMQStubService.updateStubListener(stub);
        } catch (Exception e) {
            log.warn("Could not fully initialize queue connections for stub '{}': {}",
                    stub.getName(), e.getMessage());
            // Don't fail the entire initialization for connection issues
        }
    }

    private Set<String> discoverServerNames() {
        Set<String> serverNames = ConcurrentHashMap.newKeySet();

        if (environment instanceof ConfigurableEnvironment configurableEnv) {
            configurableEnv.getPropertySources().forEach(propertySource -> {
                if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
                    for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                        if (propertyName.startsWith(REGISTRY_PREFIX)) {
                            Matcher matcher = SERVER_NAME_PATTERN.matcher(propertyName);
                            if (matcher.matches()) {
                                serverNames.add(matcher.group(1));
                            }
                        }
                    }
                }
            });
        }
        return serverNames;
    }

    private ActiveMqServerConfig loadServerConfig(String serverName) {
        String prefix = REGISTRY_PREFIX + serverName + ".";

        String brokerUrl = getRequiredProperty(prefix + "broker.url", serverName);
        String username = getProperty(prefix + "username");
        String password = getProperty(prefix + "password");
        Integer timeout = getIntegerProperty(prefix + "timeout");
        Boolean enabled = getBooleanProperty(prefix + "enabled");

        return new ActiveMqServerConfig(serverName, brokerUrl, username, password, timeout, enabled);
    }

    private String getRequiredProperty(String propertyName, String serverName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Required ActiveMQ property '" + propertyName + "' is missing or empty for server: " + serverName
            );
        }
        return value;
    }

    private String getProperty(String propertyName) {
        return environment.getProperty(propertyName);
    }

    private Integer getIntegerProperty(String propertyName) {
        String value = environment.getProperty(propertyName);
        return (value != null && !value.trim().isEmpty()) ? Integer.parseInt(value.trim()) : null;
    }

    private Boolean getBooleanProperty(String propertyName) {
        String value = environment.getProperty(propertyName);
        return (value != null && !value.trim().isEmpty()) ? Boolean.parseBoolean(value.trim()) : false;
    }

    public ActiveMqServerConfig getServerConfig(String serverName) {
        ActiveMqServerConfig config = serverConfigs.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("ActiveMQ server '" + serverName + "' not found in registry.");
        }
        return config;
    }

    public boolean hasServer(String serverName) {
        return serverConfigs.containsKey(serverName);
    }

    public List<String> getAvailableServerNames() {
        return Collections.unmodifiableList(serverConfigs.keySet().stream().sorted().collect(Collectors.toList()));
    }

    public Map<String, ActiveMqServerConfig> getAllServers() {
        return Collections.unmodifiableMap(serverConfigs);
    }

    public int getServerCount() {
        return serverConfigs.size();
    }

    public boolean isEmpty() {
        return serverConfigs.isEmpty();
    }
} 
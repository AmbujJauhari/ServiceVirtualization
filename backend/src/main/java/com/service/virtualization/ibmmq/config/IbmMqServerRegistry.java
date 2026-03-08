package com.service.virtualization.ibmmq.config;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.service.IBMMQStubService;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Registry for multiple IBM MQ server configurations.
 * Discovers server configurations from environment properties at application startup
 * and initializes all active stubs.
 * 
 * Server properties should follow the pattern:
 * {@code ibmmq.registry.<serverName>.<property>}
 * 
 * Example:
 * <pre>
 * IBMMQ_REGISTRY_SERVERA_HOST=ibmmq-servera.company.com
 * IBMMQ_REGISTRY_SERVERA_PORT=1414
 * IBMMQ_REGISTRY_SERVERA_QUEUE_MANAGER=QM_A
 * IBMMQ_REGISTRY_SERVERA_CHANNEL=CHANNEL.A
 * </pre>
 */
@Component
@Profile("!ibmmq-disabled")
public class IbmMqServerRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(IbmMqServerRegistry.class);
    private static final String REGISTRY_PREFIX = "ibmmq.registry.";
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("^ibmmq\\.registry\\.([^.]+)\\..*");
    
    private final Environment environment;
    private final IBMMQStubService ibmMQStubService;
    private final Map<String, IbmMqServerConfig> serverConfigs = new ConcurrentHashMap<>();
    
    @Autowired
    public IbmMqServerRegistry(Environment environment, @Lazy IBMMQStubService ibmMQStubService) {
        this.environment = environment;
        this.ibmMQStubService = ibmMQStubService;
    }
    
    /**
     * Populate server configs eagerly during bean construction so they are available
     * when {@link IbmMqConnectionFactoryRegistry} runs its {@code ApplicationReadyEvent} handler.
     */
    @PostConstruct
    public void postConstruct() {
        try {
            initializeServerRegistry();
        } catch (Exception e) {
            log.warn("⚠️  IBM MQ server registry initialization failed: {}. IBM MQ will be unavailable.", e.getMessage());
        }
    }

    /**
     * After the whole application context is ready, kick off the retry-scheduler that
     * reconnects any existing active stubs that were stored in the database.
     * Never throws — an unreachable IBM MQ broker must not prevent the application from starting.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (isEmpty()) {
            log.info("📋 IBM MQ Server Registry is empty — skipping active stub initialization.");
            return;
        }
        initializeActiveStubsWithRetry();
    }

    private void initializeActiveStubsWithRetry() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ibmmq-stub-retry");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                List<IBMMQStub> pending = ibmMQStubService.findByStatus(StubStatus.ACTIVE);
                if (pending.isEmpty()) {
                    scheduler.shutdown();
                    return;
                }
                int connected = 0;
                for (IBMMQStub stub : pending) {
                    try {
                        ibmMQStubService.updateStubListener(stub);
                        connected++;
                    } catch (Exception e) {
                        log.debug("⏳ IBM MQ stub '{}' not yet connected: {}", stub.getName(), e.getMessage());
                    }
                }
                if (connected == pending.size()) {
                    log.info("✅ All {} IBM MQ stub listener(s) connected.", connected);
                    scheduler.shutdown();
                } else {
                    log.info("⏳ IBM MQ: {}/{} stub(s) connected, retrying in 30 s...", connected, pending.size());
                }
            } catch (Exception e) {
                log.debug("⏳ IBM MQ not yet reachable, will retry in 30 s: {}", e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Initialize the multi-server registry.
     */
    private void initializeServerRegistry() {
        log.info("🔧 Initializing IBM MQ Server Registry...");
        
        Set<String> serverNames = discoverServerNames();
        
        if (serverNames.isEmpty()) {
            log.info("📋 No IBM MQ multi-server configurations found (using legacy single-server mode if configured).");
            return;
        }
        
        log.info("📋 Discovered {} IBM MQ server(s): {}", serverNames.size(), serverNames);
        
        for (String serverName : serverNames) {
            try {
                IbmMqServerConfig config = loadServerConfig(serverName);
                config.validate();
                serverConfigs.put(serverName, config);
                log.info("✅ Registered IBM MQ server '{}': {}:{}", serverName, config.getHost(), config.getPort());
            } catch (IllegalStateException e) {
                log.error("❌ Failed to register IBM MQ server '{}': {}", serverName, e.getMessage());
                throw new IllegalStateException("Invalid IBM MQ server configuration for '" + serverName + "': " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("❌ Unexpected error during IBM MQ server '{}' initialization: {}", serverName, e.getMessage(), e);
                throw new IllegalStateException("Unexpected error during IBM MQ server '" + serverName + "' initialization: " + e.getMessage(), e);
            }
        }
        
        log.info("🎉 IBM MQ Server Registry initialized successfully with {} server(s)", serverConfigs.size());
    }
    

    /**
     * Validate the configuration of an IBM MQ stub.
     *
     * @param stub The stub to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateStubConfiguration(IBMMQStub stub) {
        if (stub.getName() == null || stub.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stub name cannot be empty");
        }

        // Validate content matching configuration
        if (stub.getContentMatchType() != null && stub.getContentMatchType() != IBMMQStub.ContentMatchType.NONE) {
            if (stub.getContentPattern() == null || stub.getContentPattern().trim().isEmpty()) {
                log.warn("⚠️ Content matching type is set but pattern is empty for stub: {}", stub.getName());
            }
        }

        // Validate response configuration
        if (stub.getResponseContent() != null && !stub.getResponseContent().trim().isEmpty()) {
            if (stub.getResponseDestination() == null || stub.getResponseDestination().trim().isEmpty()) {
                log.debug("📝 Response destination not specified for stub '{}', will use JMSReplyTo", stub.getName());
            }
        }

        log.debug("✅ Configuration validation passed for stub: {}", stub.getName());
    }

    /**
     * Initialize queue connections for the stub.
     *
     * @param stub The stub to initialize connections for
     */
    private void initializeQueueConnections(IBMMQStub stub) {
        try {
            // Update the stub listener to start listening on the configured destination
            ibmMQStubService.updateStubListener(stub);
        } catch (Exception e) {
            log.warn("⚠️ Could not fully initialize queue connections for stub '{}': {}",
                    stub.getName(), e.getMessage());
            // Don't fail the entire initialization for connection issues
        }
    }
    
    private Set<String> discoverServerNames() {
        Set<String> serverNames = ConcurrentHashMap.newKeySet();
        
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            configurableEnv.getPropertySources().forEach(propertySource -> {
                if (propertySource instanceof org.springframework.core.env.EnumerablePropertySource) {
                    org.springframework.core.env.EnumerablePropertySource<?> enumerablePropertySource = 
                        (org.springframework.core.env.EnumerablePropertySource<?>) propertySource;
                    
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
    
    private IbmMqServerConfig loadServerConfig(String serverName) {
        String prefix = REGISTRY_PREFIX + serverName + ".";

        String host = getRequiredProperty(prefix + "host", serverName);
        Integer port = getIntegerProperty(prefix + "port", null);
        if (port == null) {
            throw new IllegalStateException("Required IBM MQ property '" + prefix + "port' is missing for server: " + serverName);
        }
        String queueManager = getRequiredProperty(prefix + "queue.manager", serverName);
        String channel = getRequiredProperty(prefix + "channel", serverName);
        String username = getProperty(prefix + "username");
        String password = getProperty(prefix + "password");
        Integer timeout = getIntegerProperty(prefix + "timeout", 30000);
        Boolean enabled = getBooleanProperty(prefix + "enabled", true);

        // SSL — env vars: IBMMQ_REGISTRY_<NAME>_SSL_ENABLED, IBMMQ_REGISTRY_<NAME>_SSL_CIPHER_SUITE
        Boolean sslEnabled = getBooleanProperty(prefix + "ssl.enabled", false);
        String sslCipherSuite = getProperty(prefix + "ssl.cipher.suite");

        return new IbmMqServerConfig(serverName, host, port, queueManager, channel,
                                     username, password, timeout, enabled,
                                     sslEnabled, sslCipherSuite);
    }
    
    private String getRequiredProperty(String propertyName, String serverName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "Required IBM MQ property '" + propertyName + "' is missing or empty for server: " + serverName
            );
        }
        return value;
    }
    
    private String getProperty(String propertyName) {
        return environment.getProperty(propertyName);
    }
    
    private Integer getIntegerProperty(String propertyName, Integer defaultValue) {
        String value = environment.getProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer format for property '{}': {}. Using default value: {}", 
                        propertyName, value, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private Boolean getBooleanProperty(String propertyName, Boolean defaultValue) {
        String value = environment.getProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    public IbmMqServerConfig getServerConfig(String serverName) {
        IbmMqServerConfig config = serverConfigs.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("IBM MQ server '" + serverName + "' not found in registry.");
        }
        return config;
    }
    
    public boolean hasServer(String serverName) {
        return serverConfigs.containsKey(serverName);
    }
    
    public List<String> getAvailableServerNames() {
        return Collections.unmodifiableList(serverConfigs.keySet().stream().sorted().collect(Collectors.toList()));
    }
    
    public Map<String, IbmMqServerConfig> getAllServers() {
        return Collections.unmodifiableMap(serverConfigs);
    }
    
    public int getServerCount() {
        return serverConfigs.size();
    }
    
    public boolean isEmpty() {
        return serverConfigs.isEmpty();
    }
} 
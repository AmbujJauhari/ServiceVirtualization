package com.service.virtualization.tibco.config;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.tibco.jakarta.JavaxToJakartaConnectionFactoryAdapter;
import com.service.virtualization.tibco.listener.TibcoDynamicDestinationManager;
import com.service.virtualization.tibco.model.TibcoStub;
import com.service.virtualization.tibco.repository.TibcoStubRepository;
import com.tibco.tibjms.TibjmsConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry for managing multiple TIBCO EMS server configurations.
 * Handles both server initialization and active stub registration.
 * <p>
 * Two-step initialization process:
 * 1. Load server configurations and establish connections
 * 2. Register all active stubs from the database
 * <p>
 * Dynamically loads server configurations from environment variables using the pattern:
 * - tibco.registry.{serverName}.url
 * - tibco.registry.{serverName}.username
 * - tibco.registry.{serverName}.password
 * - tibco.registry.{serverName}.timeout
 * - tibco.registry.{serverName}.ssl.enabled
 * - tibco.registry.{serverName}.ssl.jks.path
 * - tibco.registry.{serverName}.ssl.jks.password
 * - tibco.registry.{serverName}.ssl.truststore.path
 * - tibco.registry.{serverName}.ssl.truststore.password
 * <p>
 * Example configuration for 3 servers:
 * <pre>
 * # Server A - SSL enabled
 * TIBCO_REGISTRY_SERVERA_URL=ssl://tibco-servera.company.com:7243
 * TIBCO_REGISTRY_SERVERA_USERNAME=admin
 * TIBCO_REGISTRY_SERVERA_PASSWORD=secret
 * TIBCO_REGISTRY_SERVERA_SSL_ENABLED=true
 * TIBCO_REGISTRY_SERVERA_SSL_JKS_PATH=/tibco/certs/serverA/client.jks
 * TIBCO_REGISTRY_SERVERA_SSL_JKS_PASSWORD=jks-pass
 * TIBCO_REGISTRY_SERVERA_TIMEOUT=5000
 *
 * # Server B - Non-SSL
 * TIBCO_REGISTRY_SERVERB_URL=tcp://tibco-serverb.company.com:7222
 * TIBCO_REGISTRY_SERVERB_USERNAME=admin
 * TIBCO_REGISTRY_SERVERB_PASSWORD=secret
 * TIBCO_REGISTRY_SERVERB_SSL_ENABLED=false
 * TIBCO_REGISTRY_SERVERB_TIMEOUT=5000
 * </pre>
 */
@Component
@Profile("!tibco-disabled")
public class TibcoServerRegistry {

    private static final Logger log = LoggerFactory.getLogger(TibcoServerRegistry.class);
    private static final String REGISTRY_PREFIX = "tibco.registry.";
    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("^tibco\\.registry\\.([^.]+)\\..*");

    private final Environment environment;
    private final TibcoStubRepository tibcoStubRepository;
    private final TibcoDynamicDestinationManager destinationManager;
    
    @Value("${tibco.connection.cache-size:10}")
    private int sessionCacheSize;

    /**
     * Map of server name -> configuration
     * Thread-safe for concurrent access
     */
    private final Map<String, TibcoServerConfig> servers = new ConcurrentHashMap<>();
    
    /**
     * Map of server name -> connection factory
     * Thread-safe for concurrent access
     */
    private final Map<String, ConnectionFactory> connectionFactories = new ConcurrentHashMap<>();

    @Autowired
    public TibcoServerRegistry(Environment environment, 
                               TibcoStubRepository tibcoStubRepository,
                               @Lazy TibcoDynamicDestinationManager destinationManager) {
        this.environment = environment;
        this.tibcoStubRepository = tibcoStubRepository;
        this.destinationManager = destinationManager;
    }

    /**
     * Eagerly loads server configurations during bean construction so that
     * connection factory registries that run on {@code ApplicationReadyEvent} see a populated registry.
     */
    @PostConstruct
    public void postConstruct() {
        try {
            initializeServerRegistry();
        } catch (Exception e) {
            log.warn("⚠️  TIBCO server registry initialization failed: {}. TIBCO will be unavailable.", e.getMessage());
        }
    }

    /**
     * After the whole application context is ready, start the retry-scheduler
     * that reconnects any existing active stubs stored in the database.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (isEmpty()) {
            log.info("📋 TIBCO Server Registry is empty — skipping active stub initialization.");
            return;
        }
        initializeActiveStubsWithRetry();
    }

    private void initializeActiveStubsWithRetry() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tibco-stub-retry");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                List<TibcoStub> pending = tibcoStubRepository.findByStatus(StubStatus.ACTIVE);
                if (pending.isEmpty()) {
                    scheduler.shutdown();
                    return;
                }
                int connected = 0;
                for (TibcoStub stub : pending) {
                    try {
                        boolean ok = destinationManager.registerListener(stub);
                        if (ok) connected++;
                    } catch (Exception e) {
                        log.debug("⏳ TIBCO stub '{}' not yet connected: {}", stub.getName(), e.getMessage());
                    }
                }
                if (connected == pending.size()) {
                    log.info("✅ All {} TIBCO stub listener(s) connected.", connected);
                    scheduler.shutdown();
                } else {
                    log.info("⏳ TIBCO: {}/{} stub(s) connected, retrying in 30 s...", connected, pending.size());
                }
            } catch (Exception e) {
                log.debug("⏳ TIBCO not yet reachable, will retry in 30 s: {}", e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Step 1: Initializes the registry by loading all server configurations
     * and creating connection factories.
     */
    private void initializeServerRegistry() {
        log.info("🔧 Initializing TIBCO Server Registry...");

        try {
            // Discover all server names from properties
            Set<String> serverNames = discoverServerNames();

            if (serverNames.isEmpty()) {
                log.warn("📌 No TIBCO servers configured in registry (no tibco.registry.* properties found)");
                log.warn("TIBCO functionality will not be available");
                return;
            }

            log.info("📋 Discovered {} TIBCO server(s): {}", serverNames.size(), serverNames);

            // Load configuration and create connection factory for each server
            for (String serverName : serverNames) {
                try {
                    // Load server configuration
                    TibcoServerConfig config = loadServerConfig(serverName);

                    // Validate configuration
                    config.validate();

                    // Register server
                    servers.put(serverName, config);

                    log.info("✅ Registered TIBCO server '{}': {} (SSL: {})",
                            serverName,
                            config.getUrl(),
                            config.isSslEnabled() ? "enabled" : "disabled"
                    );
                    
                    // Create connection factory if server is enabled
                    if (!config.getEnabled()) {
                        log.info("⏭️  Skipping connection factory creation for disabled server: {}", serverName);
                        continue;
                    }
                    
                    ConnectionFactory factory = createConnectionFactory(config);
                    connectionFactories.put(serverName, factory);
                    
                    log.info("✅ Created connection factory for server '{}'", serverName);

                } catch (Exception e) {
                    // A failed server does not block startup or other servers.
                    log.warn("⚠️  TIBCO server '{}' could not be initialized (will still be retried for stubs): {}", serverName, e.getMessage());
                }
            }

            log.info("🎉 TIBCO Server Registry ready with {}/{} server(s) initialized", connectionFactories.size(), servers.size());

        } catch (Exception e) {
            log.warn("⚠️  TIBCO server registry encountered an error: {}", e.getMessage());
            throw e; // re-throw so initialize() can catch and log the skip
        }
    }

    /**
     * Validate the configuration of a TIBCO stub.
     *
     * @param stub The stub to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateStubConfiguration(TibcoStub stub) {
        if (stub.getName() == null || stub.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stub name cannot be empty");
        }

        if (stub.getDestinationName() == null || stub.getDestinationName().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination name cannot be empty for stub: " + stub.getName());
        }

        // Validate server name if using multi-server mode
        String serverName = stub.getServerName();
        if (serverName != null && !serverName.trim().isEmpty()) {
            if (!hasServer(serverName)) {
                throw new IllegalArgumentException(
                    "Server '" + serverName + "' not found in registry for stub: " + stub.getName() +
                    ". Available servers: " + getAvailableServerNames()
                );
            }
        }
    }
    
    /**
     * Creates a connection factory for a specific TIBCO server.
     */
    private ConnectionFactory createConnectionFactory(TibcoServerConfig config) throws Exception {
        // Create javax TIBCO connection factory
        TibjmsConnectionFactory javaxConnectionFactory = new TibjmsConnectionFactory();
        javaxConnectionFactory.setServerUrl(config.getUrl());
        javaxConnectionFactory.setUserName(config.getUsername());
        javaxConnectionFactory.setUserPassword(config.getPassword());
        
        // Set connection timeout
        int timeout = config.getTimeout() != null ? config.getTimeout() : 30000;
        javaxConnectionFactory.setConnAttemptTimeout(timeout);
        javaxConnectionFactory.setReconnAttemptTimeout(timeout);
        
        // Configure SSL if enabled
        if (config.isSslEnabled() && config.getSsl() != null) {
            configureSsl(javaxConnectionFactory, config);
        }
        
        // Wrap with Jakarta compatibility adapter
        ConnectionFactory jakartaConnectionFactory =
                new JavaxToJakartaConnectionFactoryAdapter(javaxConnectionFactory);
        
        // Wrap with caching connection factory for performance
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(jakartaConnectionFactory);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setReconnectOnException(true);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(true);
        
        return cachingConnectionFactory;
    }
    
    /**
     * Configures SSL for a TIBCO connection factory using TIBCO's native SSL setters.
     *
     * Strategy:
     *  - setSSLVendor("j2se"): tells TIBCO to use the standard Java SSL stack.
     *  - Truststore: if tibco.registry.<name>.ssl.truststore.path is set, point TIBCO to that
     *    JKS; otherwise fall back to the JVM's javax.net.ssl.trustStore system property
     *    (set via JAVA_OPTS in the pod's backend-values.yaml).
     *  - Client keystore (mutual TLS): only configured when jks.path is explicitly provided.
     *  - Hostname verification: disabled when verifyHostname=false so that internal
     *    cluster service names (e.g. *.svc.cluster.local) are accepted with the
     *    wildcard certificate.
     */
    private void configureSsl(TibjmsConnectionFactory factory, TibcoServerConfig config) throws Exception {
        TibcoServerConfig.SslConfig sslConfig = config.getSsl();

        // Delegate SSL entirely to the JVM's JSSE stack.
        // All javax.net.ssl.* properties (set via JAVA_OPTS) are honoured automatically.
        factory.setSSLVendor("j2se");

        // Explicit truststore override — only needed when a per-server JKS path is set.
        // In the normal case the global truststore is already set via JAVA_OPTS in the pod,
        // so there is nothing extra to do here.
        if (sslConfig != null &&
                sslConfig.getTruststorePath() != null &&
                !sslConfig.getTruststorePath().trim().isEmpty()) {
            // With j2se vendor, TIBCO honours javax.net.ssl System properties.
            System.setProperty("javax.net.ssl.trustStore", sslConfig.getTruststorePath());
            if (sslConfig.getTruststorePassword() != null) {
                System.setProperty("javax.net.ssl.trustStorePassword", sslConfig.getTruststorePassword());
            }
            log.info("TIBCO SSL truststore configured for server '{}': {}",
                    config.getName(), sslConfig.getTruststorePath());
        } else {
            log.info("TIBCO SSL for server '{}' will use JVM javax.net.ssl.trustStore (set via JAVA_OPTS)",
                    config.getName());
        }

        // Client keystore — only for mutual TLS (ssl_require_client_cert = true on the TIBCO server)
        if (sslConfig != null &&
                sslConfig.getJksPath() != null &&
                !sslConfig.getJksPath().trim().isEmpty()) {
            factory.setSSLIdentity(sslConfig.getJksPath());
            factory.setSSLPassword(sslConfig.getJksPassword());
            log.info("TIBCO mutual TLS keystore configured for server '{}': {}",
                    config.getName(), sslConfig.getJksPath());
        }

        // Hostname verification: with j2se vendor, verification is performed by the JVM's
        // standard JSSE stack. The wildcard cert (*.service-virtualization.local) covers all
        // external hostnames used by this service, so no override is needed.
        // Logging only when explicitly disabled for diagnostic clarity.
        if (sslConfig != null && Boolean.FALSE.equals(sslConfig.getVerifyHostname())) {
            log.warn("TIBCO SSL hostname verification disabled for server '{}' — ensure the wildcard cert covers the server hostname", config.getName());
        }
    }

    /**
     * Discovers all unique server names from environment properties.
     *
     * @return Set of server names (e.g., "serverA", "serverB")
     */
    private Set<String> discoverServerNames() {
        Set<String> serverNames = new HashSet<>();

        // Get all property names from environment
        for (String propertyName : getAllPropertyNames()) {
            if (propertyName.startsWith(REGISTRY_PREFIX)) {
                Matcher matcher = SERVER_NAME_PATTERN.matcher(propertyName);
                if (matcher.matches()) {
                    String serverName = matcher.group(1);
                    serverNames.add(serverName);
                }
            }
        }

        return serverNames;
    }

    /**
     * Gets all property names from the environment.
     * Handles both property file and environment variable sources.
     */
    private List<String> getAllPropertyNames() {
        List<String> propertyNames = new ArrayList<>();

        // Get from Spring Environment (handles both property files and env vars)
        org.springframework.core.env.MutablePropertySources propertySources =
                ((org.springframework.core.env.ConfigurableEnvironment) environment).getPropertySources();

        propertySources.forEach(propertySource -> {
            if (propertySource instanceof org.springframework.core.env.EnumerablePropertySource) {
                String[] names = ((org.springframework.core.env.EnumerablePropertySource<?>) propertySource).getPropertyNames();
                propertyNames.addAll(Arrays.asList(names));
            }
        });

        return propertyNames;
    }

    /**
     * Loads configuration for a specific server from environment properties.
     *
     * @param serverName Name of the server (e.g., "serverA")
     * @return TibcoServerConfig instance
     */
    private TibcoServerConfig loadServerConfig(String serverName) {
        String prefix = REGISTRY_PREFIX + serverName + ".";

        // Load basic configuration
        String url = getRequiredProperty(prefix + "url", serverName);
        String username = getRequiredProperty(prefix + "username", serverName);
        String password = getRequiredProperty(prefix + "password", serverName);

        // Load optional configuration
        Integer timeout = getIntegerProperty(prefix + "timeout");
        Boolean enabled = getBooleanProperty(prefix + "enabled");

        // Load SSL configuration (only if needed)
        TibcoServerConfig.SslConfig sslConfig = null;
        if (url.toLowerCase().startsWith("ssl://")) {
            sslConfig = loadSslConfig(serverName, prefix);
        }

        // Build config
        return new TibcoServerConfig(serverName, url, username, password, timeout, sslConfig, enabled);
    }

    /**
     * Loads SSL configuration for a server.
     * Only called if the server URL uses ssl://
     *
     * @param serverName Name of the server
     * @param prefix     Property prefix (e.g., "tibco.registry.serverA.")
     * @return SslConfig instance
     */
    private TibcoServerConfig.SslConfig loadSslConfig(String serverName, String prefix) {
        String sslPrefix = prefix + "ssl.";

        // SSL is enabled by default if URL uses ssl://
        Boolean sslEnabled = getBooleanProperty(sslPrefix + "enabled");

        // JKS configuration (required for SSL)
        String jksPath = getProperty(sslPrefix + "jks.path");
        String jksPassword = getProperty(sslPrefix + "jks.password");

        // Truststore configuration (optional)
        String truststorePath = getProperty(sslPrefix + "truststore.path");
        String truststorePassword = getProperty(sslPrefix + "truststore.password");

        // SSL protocol and hostname verification (optional)
        String protocol = getProperty(sslPrefix + "protocol", "TLSv1.2");
        Boolean verifyHostname = getBooleanProperty(sslPrefix + "verify.hostname");

        return new TibcoServerConfig.SslConfig(
                sslEnabled,
                jksPath,
                jksPassword,
                truststorePath,
                truststorePassword,
                protocol,
                verifyHostname
        );
    }

    /**
     * Gets a required property, throwing exception if not found.
     */
    private String getRequiredProperty(String key, String serverName) {
        String value = environment.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Required property not found for TIBCO server '" + serverName + "': " + key +
                            "\nEnsure all required properties are set!"
            );
        }
        return value.trim();
    }

    /**
     * Gets an optional property with a default value.
     */
    private String getProperty(String key, String defaultValue) {
        String value = environment.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    /**
     * Gets an optional property (may be null).
     */
    private String getProperty(String key) {
        String value = environment.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    /**
     * Gets an integer property, returning null if absent or empty.
     */
    private Integer getIntegerProperty(String key) {
        String value = environment.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? Integer.parseInt(value.trim()) : null;
    }

    /**
     * Gets a boolean property, returning false if absent or empty.
     */
    private Boolean getBooleanProperty(String key) {
        String value = environment.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? Boolean.parseBoolean(value.trim()) : false;
    }

    // ========== Public API ==========

    /**
     * Gets configuration for a specific server.
     *
     * @param serverName Name of the server
     * @return Server configuration
     * @throws IllegalArgumentException if server not found
     */
    public TibcoServerConfig getServer(String serverName) {
        TibcoServerConfig config = servers.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException(
                    "TIBCO server '" + serverName + "' not found in registry. " +
                            "Available servers: " + getAvailableServerNames()
            );
        }
        return config;
    }

    /**
     * Checks if a server exists in the registry.
     *
     * @param serverName Name of the server
     * @return true if server exists
     */
    public boolean hasServer(String serverName) {
        return servers.containsKey(serverName);
    }

    /**
     * Gets all registered server configurations.
     *
     * @return Map of server name -> configuration
     */
    public Map<String, TibcoServerConfig> getAllServers() {
        return Collections.unmodifiableMap(servers);
    }

    /**
     * Gets list of available server names.
     *
     * @return List of server names
     */
    public List<String> getAvailableServerNames() {
        return new ArrayList<>(servers.keySet());
    }

    /**
     * Gets count of registered servers.
     *
     * @return Number of servers
     */
    public int getServerCount() {
        return servers.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no servers are configured
     */
    public boolean isEmpty() {
        return servers.isEmpty();
    }
    
    // ========== Connection Factory API ==========
    
    /**
     * Gets connection factory for a specific server.
     * 
     * @param serverName Name of the server
     * @return Connection factory for the server
     * @throws IllegalArgumentException if server not found
     */
    public ConnectionFactory getConnectionFactory(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            // Return default factory if no server specified (backward compatibility)
            return getDefaultConnectionFactory();
        }
        
        ConnectionFactory factory = connectionFactories.get(serverName);
        if (factory == null) {
            throw new IllegalArgumentException(
                "Connection factory not found for TIBCO server '" + serverName + "'. " +
                "Available servers: " + connectionFactories.keySet()
            );
        }
        return factory;
    }
    
    /**
     * Gets the default connection factory (for backward compatibility).
     */
    public ConnectionFactory getDefaultConnectionFactory() {
        // If multi-server mode, return the first factory
        if (!connectionFactories.isEmpty()) {
            return connectionFactories.values().iterator().next();
        }
        
        throw new IllegalStateException(
            "No TIBCO connection factory available. " +
            "Ensure TIBCO is configured correctly."
        );
    }
    
    /**
     * Checks if a connection factory exists for a server.
     */
    public boolean hasConnectionFactory(String serverName) {
        return connectionFactories.containsKey(serverName);
    }
    
    /**
     * Gets all registered server names with connection factories.
     */
    public Set<String> getConnectionFactoryServerNames() {
        return connectionFactories.keySet();
    }
    
    /**
     * Checks if registry is in multi-server mode.
     */
    public boolean isMultiServerMode() {
        return !connectionFactories.isEmpty();
    }
} 
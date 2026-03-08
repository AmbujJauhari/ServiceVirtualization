package com.service.virtualization.ibmmq.config;

import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing multiple IBM MQ connection factories.
 * 
 * Creates and maintains connection factories for each configured IBM MQ server.
 */
@Component
@Profile("!ibmmq-disabled")
public class IbmMqConnectionFactoryRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(IbmMqConnectionFactoryRegistry.class);
    
    @Autowired
    private IbmMqServerRegistry serverRegistry;
    
    private final Map<String, ConnectionFactory> connectionFactories = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("🔧 Initializing IBM MQ Connection Factory Registry...");
        try {
            if (serverRegistry.isEmpty()) {
                log.warn("⚠️  No IBM MQ servers configured (ibmmq.registry.*). IBM MQ functionality will be unavailable.");
                return;
            }
            for (Map.Entry<String, IbmMqServerConfig> entry : serverRegistry.getAllServers().entrySet()) {
                String serverName = entry.getKey();
                IbmMqServerConfig config = entry.getValue();
                if (!config.getEnabled()) {
                    log.info("⏭️  Skipping disabled IBM MQ server: {}", serverName);
                    continue;
                }
                try {
                    ConnectionFactory factory = createConnectionFactory(config);
                    connectionFactories.put(serverName, factory);
                    log.info("✅ Created IBM MQ connection factory for server '{}': {}:{}", serverName, config.getHost(), config.getPort());
                } catch (Exception e) {
                    // A failed factory prevents listening on that server, but does not block app startup.
                    log.warn("⚠️  Could not create IBM MQ connection factory for server '{}': {}. " +
                             "Other protocols remain operational. IBM MQ stub listeners for this server will be unavailable.",
                             serverName, e.getMessage());
                }
            }
            log.info("🎉 IBM MQ Connection Factory Registry ready with {} server(s)", connectionFactories.size());
        } catch (Exception e) {
            // Never propagate from @EventListener — a failed IBM MQ setup must not crash the application.
            log.warn("⚠️  IBM MQ Connection Factory Registry initialization encountered an error: {}. " +
                     "IBM MQ functionality will be unavailable until the issue is resolved.", e.getMessage());
        }
    }
    
    private ConnectionFactory createConnectionFactory(IbmMqServerConfig config) throws JMSException {
        MQQueueConnectionFactory factory = new MQQueueConnectionFactory();

        factory.setHostName(config.getHost());
        factory.setPort(config.getPort());
        factory.setQueueManager(config.getQueueManager());
        factory.setChannel(config.getChannel());
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208); // UTF-8

        // SSL — the truststore must be present at the path set in javax.net.ssl.trustStore
        // (injected into the JVM via JAVA_OPTS in the pod's backend-values.yaml).
        // The cipher suite here must match the SSLCIPH on the server-connection channel.
        if (config.isSslEnabled()) {
            factory.setSSLCipherSuite(config.getSslCipherSuite());
            log.info("🔒 SSL enabled for IBM MQ server '{}' with cipher suite: {}",
                    config.getName(), config.getSslCipherSuite());
        }

        if (config.getUsername() != null && !config.getUsername().trim().isEmpty()) {
            factory.setStringProperty(WMQConstants.USERID, config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().trim().isEmpty()) {
            factory.setStringProperty(WMQConstants.PASSWORD, config.getPassword());
        }

        return factory;
    }
    
    // ========== Public API ==========
    
    public ConnectionFactory getConnectionFactory(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return getDefaultConnectionFactory();
        }
        
        ConnectionFactory factory = connectionFactories.get(serverName);
        if (factory == null) {
            throw new IllegalArgumentException(
                "Connection factory not found for IBM MQ server '" + serverName + "'. " +
                "Available servers: " + connectionFactories.keySet()
            );
        }
        return factory;
    }
    
    public ConnectionFactory getDefaultConnectionFactory() {
        if (!connectionFactories.isEmpty()) {
            return connectionFactories.values().iterator().next();
        }
        throw new IllegalStateException(
            "No IBM MQ connection factory available. " +
            "Configure at least one server via ibmmq.registry.<name>.*"
        );
    }
    
    public boolean hasConnectionFactory(String serverName) {
        return connectionFactories.containsKey(serverName);
    }
    
    public java.util.Set<String> getServerNames() {
        return connectionFactories.keySet();
    }
    
    public boolean isMultiServerMode() {
        return !connectionFactories.isEmpty();
    }
} 
package com.service.virtualization.activemq.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing multiple ActiveMQ connection factories.
 * 
 * Creates and maintains connection factories for each configured ActiveMQ server.
 */
@Component
@Profile("!activemq-disabled")
public class ActiveMqConnectionFactoryRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ActiveMqConnectionFactoryRegistry.class);
    
    @Autowired
    private ActiveMqServerRegistry serverRegistry;
    
    @Value("${activemq.connection.cache-size:10}")
    private int sessionCacheSize;
    
    private final Map<String, ConnectionFactory> connectionFactories = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("Initializing ActiveMQ Connection Factory Registry...");
        
        try {
            if (serverRegistry.isEmpty()) {
                log.warn("⚠️  No ActiveMQ servers configured (activemq.registry.*). ActiveMQ functionality will be unavailable.");
                return;
            }

            for (Map.Entry<String, ActiveMqServerConfig> entry : serverRegistry.getAllServers().entrySet()) {
                String serverName = entry.getKey();
                ActiveMqServerConfig config = entry.getValue();

                if (!config.getEnabled()) {
                    log.info("⏭️  Skipping disabled ActiveMQ server: {}", serverName);
                    continue;
                }

                try {
                    ConnectionFactory factory = createConnectionFactory(config);
                    connectionFactories.put(serverName, factory);
                    log.info("✅ Created ActiveMQ connection factory for server '{}': {}", serverName, config.getBrokerUrl());
                } catch (Exception e) {
                    log.warn("⚠️  Could not create ActiveMQ connection factory for server '{}': {}. " +
                             "That server will be unavailable; other servers remain operational.",
                             serverName, e.getMessage());
                }
            }

            log.info("🎉 ActiveMQ Connection Factory Registry ready with {} server(s)", connectionFactories.size());

        } catch (Exception e) {
            log.warn("⚠️  ActiveMQ Connection Factory Registry initialization encountered an error: {}. " +
                     "ActiveMQ functionality will be unavailable.", e.getMessage());
        }
    }
    
    private ConnectionFactory createConnectionFactory(ActiveMqServerConfig config) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(config.getBrokerUrl());
        connectionFactory.setUserName(config.getUsername());
        connectionFactory.setPassword(config.getPassword());
        
        // Set security for dynamic destinations
        connectionFactory.setTrustAllPackages(true);

        // Wrap with caching connection factory for better performance
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(connectionFactory);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setReconnectOnException(true);

        return cachingConnectionFactory;
    }

    public ConnectionFactory getConnectionFactory(String serverName) {
        ConnectionFactory factory = connectionFactories.get(serverName);
        if (factory == null) {
            throw new IllegalArgumentException(
                "Connection factory not found for ActiveMQ server '" + serverName + "'. " +
                "Available servers: " + connectionFactories.keySet()
            );
        }
        return factory;
    }

    /**
     * Gets the default connection factory (first available, for single-server fallback).
     */
    public ConnectionFactory getDefaultConnectionFactory() {
        if (!connectionFactories.isEmpty()) {
            return connectionFactories.values().iterator().next();
        }
        throw new IllegalStateException(
            "No ActiveMQ connection factory available. Ensure ActiveMQ is configured correctly.");
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
package com.service.virtualization.activemq.listener;

import com.service.virtualization.activemq.config.ActiveMqConnectionFactoryRegistry;
import com.service.virtualization.activemq.model.ActiveMQStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic registration and unregistration of JMS listeners based on ActiveMQ stubs.
 * Supports multi-server ActiveMQ configuration.
 */
@Component
@Profile("!activemq-disabled")
public class ActiveMqDynamicDestinationManager {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMqDynamicDestinationManager.class);
    
    @Autowired
    private ActiveMqConnectionFactoryRegistry connectionFactoryRegistry;
    
    @Autowired
    private ActiveMQMessageListener messageListener;
    
    // Keep track of active listeners by stub ID
    private final Map<String, DefaultMessageListenerContainer> activeListeners = new ConcurrentHashMap<>();
    
    /**
     * Register a new JMS listener for the given stub.
     *
     * @param stub The ActiveMQ stub to register a listener for
     * @return true if registration was successful, false otherwise
     */
    public boolean registerListener(ActiveMQStub stub) {
        try {
            // If already registered, unregister first
            if (activeListeners.containsKey(stub.getId())) {
                unregisterListener(stub.getId());
            }
            
            String destinationName = stub.getDestinationName();
            boolean isTopic = "topic".equalsIgnoreCase(stub.getDestinationType());
            String serverName = stub.getServerName();
            
            // Get the appropriate connection factory for this stub's server
            ConnectionFactory connectionFactory = connectionFactoryRegistry.getConnectionFactory(serverName);
            
            logger.info("Registering listener for {} {} on server '{}': {}", 
                    isTopic ? "topic" : "queue", destinationName, 
                    serverName != null ? serverName : "default", stub.getId());
                    
            DefaultMessageListenerContainer container = createMessageListenerContainer(
                    destinationName, isTopic, connectionFactory, messageListener);
            
            // Store the selector with the listener in the MessageListener
            messageListener.registerStub(stub);
            
            // Start the container
            container.initialize();
            container.start();
            
            // Store for later cleanup
            activeListeners.put(stub.getId(), container);
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to register listener for stub {}: {}", stub.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Unregister a JMS listener for the given stub ID.
     *
     * @param stubId The ID of the stub to unregister
     */
    public void unregisterListener(String stubId) {
        DefaultMessageListenerContainer container = activeListeners.get(stubId);
        if (container != null) {
            try {
                logger.info("Unregistering listener for stub: {}", stubId);
                container.stop();
                container.destroy();
                activeListeners.remove(stubId);
                messageListener.unregisterStub(stubId);
            } catch (Exception e) {
                logger.error("Error unregistering listener for stub {}: {}", stubId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Create a message listener container for the specified destination.
     *
     * @param destinationName The name of the destination (queue or topic)
     * @param isTopic True if the destination is a topic, false for a queue
     * @param listener The message listener to attach
     * @return A configured message listener container
     */
    private DefaultMessageListenerContainer createMessageListenerContainer(
            String destinationName, boolean isTopic, ConnectionFactory connectionFactory, MessageListener listener) {
        
        DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setDestinationName(destinationName);
        container.setPubSubDomain(isTopic); // true for topics, false for queues
        container.setMessageListener(listener);
        
        // Set transaction manager for reliability
        JmsTransactionManager transactionManager = new JmsTransactionManager();
        transactionManager.setConnectionFactory(connectionFactory);
        container.setTransactionManager(transactionManager);
        
        // Configure container properties
        container.setSessionTransacted(true);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(5);
        container.setRecoveryInterval(5000);
        
        return container;
    }
} 
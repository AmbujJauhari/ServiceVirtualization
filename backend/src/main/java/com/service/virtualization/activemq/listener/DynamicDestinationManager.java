package com.service.virtualization.activemq.listener;

import com.service.virtualization.activemq.model.ActiveMQStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic registration and unregistration of JMS listeners based on ActiveMQ stubs.
 */
@Component
public class DynamicDestinationManager {
    private static final Logger logger = LoggerFactory.getLogger(DynamicDestinationManager.class);
    
    @Autowired
    @Qualifier("activemqConnectionFactory")
    private ConnectionFactory connectionFactory;
    
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
            
            logger.info("Registering listener for {} {}: {}", 
                    isTopic ? "topic" : "queue", destinationName, stub.getId());
                    
            DefaultMessageListenerContainer container = createMessageListenerContainer(
                    destinationName, isTopic, messageListener);
            
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
            String destinationName, boolean isTopic, MessageListener listener) {
        
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
package com.service.virtualization.activemq.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;

/**
 * Configuration for ActiveMQ integration.
 */
@Configuration
public class ActiveMQConfig {

    @Value("${activemq.broker-url:tcp://localhost:61616}")
    private String brokerUrl;

    @Value("${activemq.username:admin}")
    private String username;

    @Value("${activemq.password:admin}")
    private String password;

    @Value("${activemq.connection.cache-size:10}")
    private int sessionCacheSize;

    /**
     * Creates an ActiveMQ connection factory.
     */
    @Bean(name = "activemqConnectionFactory")
    public ConnectionFactory activemqConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(brokerUrl);
        connectionFactory.setUserName(username);
        connectionFactory.setPassword(password);
        
        // Set security for dynamic destinations
        connectionFactory.setTrustAllPackages(true);

        // Wrap with caching connection factory for better performance
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(connectionFactory);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setReconnectOnException(true);

        return cachingConnectionFactory;
    }
    
    /**
     * JMS template for sending messages to ActiveMQ
     */
    @Bean(name = "activemqJmsTemplate")
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(activemqConnectionFactory());
        template.setPubSubDomain(true); // Set to false for queues, true for topics
        return template;
    }
} 
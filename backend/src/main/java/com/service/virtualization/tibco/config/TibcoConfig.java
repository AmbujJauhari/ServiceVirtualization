package com.service.virtualization.tibco.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.CachingConnectionFactory;

import jakarta.jms.ConnectionFactory;
import com.tibco.tibjms.TibjmsConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Configuration for TIBCO EMS integration.
 */
@Configuration
@Profile("tibco")
public class TibcoConfig {

    @Value("${tibco.url:tcp://localhost:7222}")
    private String serverUrl;

    @Value("${tibco.username:admin}")
    private String username;

    @Value("${tibco.password:admin}")
    private String password;

    @Value("${tibco.connection.cache-size:10}")
    private int sessionCacheSize;

    /**
     * Creates a TIBCO EMS connection factory.
     *
     * @throws javax.jms.JMSException
     */
    @Bean(name = "tibcoConnectionFactory")
    public ConnectionFactory tibcoConnectionFactory() throws javax.jms.JMSException {
        TibjmsConnectionFactory connectionFactory = new TibjmsConnectionFactory();
        connectionFactory.setServerUrl(serverUrl);
        connectionFactory.setUserName(username);
        connectionFactory.setUserPassword(password);

        // Wrap with caching connection factory for better performance
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory((ConnectionFactory) connectionFactory);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setReconnectOnException(true);

        return cachingConnectionFactory;
    }

    @Bean(name = "tibcoQueueJmsTemplate")
    public JmsTemplate queueJmsTemplate(@Qualifier("tibcoConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setPubSubDomain(false); // false for queues
        return template;
    }

    /**
     * JMS template for sending messages to ActiveMQ topics
     */
    @Bean(name = "tibcoTopicJmsTemplate")
    public JmsTemplate topicJmsTemplate(@Qualifier("tibcoConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setPubSubDomain(true); // true for topics
        return template;
    }
} 

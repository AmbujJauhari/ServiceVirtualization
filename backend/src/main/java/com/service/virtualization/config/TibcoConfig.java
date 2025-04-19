package com.service.virtualization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import com.tibco.tibjms.TibjmsConnectionFactory;

/**
 * Configuration for TIBCO EMS integration.
 */
@Configuration
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
     */
    @Bean(name = "tibcoConnectionFactory")
    public ConnectionFactory tibcoConnectionFactory() throws JMSException {
        TibjmsConnectionFactory connectionFactory = new TibjmsConnectionFactory();
        connectionFactory.setServerUrl(serverUrl);
        connectionFactory.setUserName(username);
        connectionFactory.setUserPassword(password);

        // Wrap with caching connection factory for better performance
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(connectionFactory);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setReconnectOnException(true);

        return cachingConnectionFactory;
    }
} 
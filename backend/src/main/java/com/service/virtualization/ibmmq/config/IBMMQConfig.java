package com.service.virtualization.ibmmq.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
@EnableJms
public class IBMMQConfig {

    @Value("${activemq.broker-url:tcp://localhost:61616}")
    private String brokerUrl;

    @Value("${activemq.username:admin}")
    private String username;

    @Value("${activemq.password:admin}")
    private String password;

    @Value("${activemq.connection.cache-size:10}")
    private int sessionCacheSize;

    @Bean(name = "ibmmqConnectionFactory")
    public ConnectionFactory ibmmqConnectionFactory() {
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

    @Bean(name = "ibmMQTemplate")
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(ibmmqConnectionFactory());
        template.setPubSubDomain(false); // Default to queues
        return template;
    }

    @Bean(name = "ibmmqListenerContainerFactory")
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> ibmmqListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(ibmmqConnectionFactory());
        factory.setPubSubDomain(false); // Default to queues
        factory.setConcurrency("1-10"); // Set concurrency for message processing
        factory.setSessionAcknowledgeMode(jakarta.jms.Session.AUTO_ACKNOWLEDGE);
        return factory;
    }
}

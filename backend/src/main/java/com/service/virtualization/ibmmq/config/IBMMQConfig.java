package com.service.virtualization.ibmmq.config;

import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@EnableJms
public class IBMMQConfig {

    @Value("${ibmmq.broker-url:localhost")
    private String brokerUrl;

    @Value("${ibmmq.port:1414}")
    private Integer port;

    @Value("${ibmmq.queue.manager:QM1}")
    private String queueManager;

    @Value("${ibmmq.channel:MY.OPEN.CHANNEL}")
    private String channel;

    @Bean(name = "ibmmqConnectionFactory")
    public ConnectionFactory ibmmqConnectionFactory() throws JMSException {
        MQQueueConnectionFactory factory = new MQQueueConnectionFactory();

        // Basic connection properties only
        factory.setHostName(brokerUrl);
        factory.setPort(port);
        factory.setQueueManager(queueManager);
        factory.setChannel(channel);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208); // UTF-8

        return factory;
    }

    @Bean(name = "ibmmqQueueJmsTemplate")
    public JmsTemplate queueJmsTemplate(@Qualifier("ibmmqConnectionFactory") ConnectionFactory ibmmqConnectionFactory) {
        JmsTemplate template = new JmsTemplate(ibmmqConnectionFactory);
        template.setMessageConverter(messageConverter());
        template.setDeliveryPersistent(true);
        template.setSessionTransacted(false);
        template.setPubSubDomain(false);
        return template;
    }

    @Bean(name = "ibmmqTopicJmsTemplate")
    public JmsTemplate topicJmsTemplate(@Qualifier("ibmmqConnectionFactory") ConnectionFactory ibmmqConnectionFactory) {
        JmsTemplate template = new JmsTemplate(ibmmqConnectionFactory);
        template.setMessageConverter(messageConverter());
        template.setDeliveryPersistent(true);
        template.setSessionTransacted(false);
        template.setPubSubDomain(true);
        return template;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}

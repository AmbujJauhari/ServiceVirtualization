package com.service.virtualization.ibmmq.test;

import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Completely independent Spring Boot application to publish messages to TEST.QUEUE.1
 * Has its own IBM MQ configuration and doesn't depend on main application
 * Uses Jakarta EE dependencies for IBM MQ and runs without web server
 * Limited component scanning to prevent loading main application beans
 */
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = "com.service.virtualization.ibmmq.test")
@Configuration
public class IBMMQStandalonePublisher implements CommandLineRunner {

    @Autowired
    private JmsTemplate jmsTemplate;

    public static void main(String[] args) {
        System.out.println("🚀 Starting Independent IBM MQ Publisher (Jakarta EE)");
        System.out.println("📋 Target Queue: TEST.QUEUE.1");
        System.out.println("🔧 Using standalone configuration with Jakarta dependencies");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 Component scanning limited to test package only");
        System.out.println("🚫 MongoDB auto-configuration excluded");
        System.out.println("=" .repeat(60));
        
        SpringApplication app = new SpringApplication(IBMMQStandalonePublisher.class);
        app.setAdditionalProfiles("test-publisher");
        
        // Disable web environment completely since we just need to publish messages
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("✅ Independent Spring Boot context loaded!");
            System.out.println("🔗 IBM MQ JmsTemplate initialized independently (Jakarta EE)");
            System.out.println("🌐 Running without web server (no port conflicts)");
            System.out.println("🚫 No main application beans loaded");
            System.out.println("🚫 No MongoDB components loaded");
            System.out.println();

            // Publish different types of messages
            publishJsonMessage();
            publishXmlMessage();
            publishMultipleMessages(3);
            
            System.out.println("=" .repeat(60));
            System.out.println("✅ All messages published successfully to TEST.QUEUE.1!");
            
        } catch (Exception e) {
            System.err.println("❌ Error publishing messages: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        System.exit(0);
    }

    private void publishJsonMessage() {
        String messageId = UUID.randomUUID().toString();
        String jsonMessage = String.format("""
            {
                "messageId": "%s",
                "timestamp": "%s",
                "content": "Independent Spring Boot publisher test message",
                "Hello":
                "type": "JSON",
                "source": "IndependentSpringBootPublisher",
                "priority": "HIGH",
                "application": "standalone",
                "jmsLibrary": "jakarta",
                "webApplication": "none",
                "isolation": "no-main-app-beans"
            }
            """, messageId, LocalDateTime.now());

        jmsTemplate.send("TEST.QUEUE.1", session -> {
            try {
                var message = session.createTextMessage(jsonMessage);
                message.setJMSCorrelationID(messageId);
                message.setStringProperty("MessageType", "JSON");
                message.setStringProperty("Source", "IndependentPublisher");
                message.setStringProperty("Priority", "HIGH");
//                message.setStringProperty("JMSLibrary", "jakarta");
                message.setStringProperty("WebApplication", "none");
                message.setStringProperty("Isolation", "no-main-app-beans");
                return message;
            } catch (JMSException e) {
                throw new RuntimeException("Failed to create JSON message", e);
            }
        });
        
        System.out.println("📤 Published JSON message with ID: " + messageId);
        System.out.println("   📄 Content: " + jsonMessage.replaceAll("\\s+", " "));
        System.out.println();
    }

    private void publishXmlMessage() {
        String messageId = UUID.randomUUID().toString();
        String xmlMessage = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <testMessage>
                <messageId>%s</messageId>
                <timestamp>%s</timestamp>
                <content>Independent Spring Boot XML test message</content>
                <type>XML</type>
                <source>IndependentSpringBootPublisher</source>
                <priority>MEDIUM</priority>
                <application>standalone</application>
                <jmsLibrary>jakarta</jmsLibrary>
                <webApplication>none</webApplication>
                <isolation>no-main-app-beans</isolation>
            </testMessage>
            """, messageId, LocalDateTime.now());

        jmsTemplate.send("TEST.QUEUE.1", session -> {
            try {
                var message = session.createTextMessage(xmlMessage);
                message.setJMSCorrelationID(messageId);
                message.setStringProperty("MessageType", "XML");
                message.setStringProperty("Source", "IndependentPublisher");
                message.setStringProperty("Priority", "MEDIUM");
                message.setStringProperty("ContentType", "application/xml");
                message.setStringProperty("JMSLibrary", "jakarta");
                message.setStringProperty("WebApplication", "none");
                message.setStringProperty("Isolation", "no-main-app-beans");
                return message;
            } catch (JMSException e) {
                throw new RuntimeException("Failed to create XML message", e);
            }
        });
        
        System.out.println("📤 Published XML message with ID: " + messageId);
        System.out.println("   📄 Content: " + xmlMessage.replaceAll("\\s+", " "));
        System.out.println();
    }

    private void publishMultipleMessages(int count) {
        System.out.println("📦 Publishing " + count + " batch messages...");
        
        for (int i = 1; i <= count; i++) {
            final int messageNumber = i;
            String messageId = "STANDALONE-" + UUID.randomUUID().toString().substring(0, 8);
            String textMessage = String.format(
                "Independent batch message #%d - Test from standalone publisher - ID: %s - Timestamp: %s - Jakarta EE - No Web Server - Isolated",
                messageNumber, messageId, LocalDateTime.now()
            );
            
            jmsTemplate.send("TEST.QUEUE.1", session -> {
                try {
                    var message = session.createTextMessage(textMessage);
                    message.setJMSCorrelationID(messageId);
                    message.setStringProperty("MessageType", "TEXT");
                    message.setStringProperty("Source", "IndependentPublisher");
                    message.setStringProperty("BatchNumber", String.valueOf(messageNumber));
                    message.setStringProperty("BatchSize", String.valueOf(count));
                    message.setStringProperty("Application", "standalone");
                    message.setStringProperty("JMSLibrary", "jakarta");
                    message.setStringProperty("WebApplication", "none");
                    message.setStringProperty("Isolation", "no-main-app-beans");
                    return message;
                } catch (JMSException e) {
                    throw new RuntimeException("Failed to create batch message", e);
                }
            });
            
            System.out.println("📤 Published batch message " + messageNumber + "/" + count + " - ID: " + messageId);
        }
        System.out.println();
    }

    /**
     * Independent IBM MQ Configuration using Jakarta EE
     * Does not use any beans from the main application
     */
    @Configuration
    static class IBMMQConfig {

        @Bean
        public ConnectionFactory connectionFactory() {
            try {
                MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
                
                // IBM MQ Connection Properties
                factory.setHostName("localhost");
                factory.setPort(1414);
                factory.setQueueManager("QM1");
                factory.setChannel("MY.OPEN.CHANNEL");
                factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
                
                // Disable authentication (matching Docker setup)
                factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);
                
                System.out.println("🔧 Independent IBM MQ ConnectionFactory configured (Jakarta EE):");
                System.out.println("   📡 Host: localhost:1414");
                System.out.println("   🎯 Queue Manager: QM1");
                System.out.println("   📋 Channel: MY.OPEN.CHANNEL");
                System.out.println("   📦 Library: Jakarta EE");
                System.out.println("   🚫 Isolated from main application");
                System.out.println("   🌐 No web server required");
                
                return factory;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to create IBM MQ ConnectionFactory", e);
            }
        }

        @Bean
        public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
            JmsTemplate jmsTemplate = new JmsTemplate();
            jmsTemplate.setConnectionFactory(connectionFactory);
            jmsTemplate.setReceiveTimeout(5000);
            
            System.out.println("🔧 Independent JmsTemplate configured (Jakarta EE)");
            System.out.println("   🚫 No dependencies on main application beans");
            
            return jmsTemplate;
        }
    }
} 
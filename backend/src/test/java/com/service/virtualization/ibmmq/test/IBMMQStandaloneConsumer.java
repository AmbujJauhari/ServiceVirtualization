package com.service.virtualization.ibmmq.test;

import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.stereotype.Component;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Always-live IBM MQ Consumer for TEST.QUEUE.2
 * Continuously listens for messages using JMS listeners
 * Uses Jakarta EE dependencies and runs without web server
 * Completely independent from main application
 */
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
@EnableJms
@Configuration
public class IBMMQStandaloneConsumer implements CommandLineRunner {

    private static final String QUEUE_NAME = "TEST.QUEUE.2";

    public static void main(String[] args) {
        System.out.println("🚀 Starting Always-Live IBM MQ Consumer (Jakarta EE)");
        System.out.println("📋 Source Queue: " + QUEUE_NAME);
        System.out.println("🔧 Using continuous JMS listeners");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 Component scanning limited to consumer only");
        System.out.println("🚫 MongoDB auto-configuration excluded");
        System.out.println("⏰ Will run continuously until stopped");
        System.out.println("=" .repeat(60));
        
        SpringApplication app = new SpringApplication(IBMMQStandaloneConsumer.class);
        app.setAdditionalProfiles("test-consumer-live");
        
        // Disable web environment completely since we just need to consume messages
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("✅ Always-Live Consumer initialized!");
        System.out.println("🔗 IBM MQ JMS Listeners configured (Jakarta EE)");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 No main application beans loaded");
        System.out.println("🚫 No MongoDB components loaded");
        System.out.println("🚫 No publisher beans loaded");
        System.out.println();
        System.out.println("👂 Now listening for messages on " + QUEUE_NAME + "...");
        System.out.println("📊 Message counter initialized");
        System.out.println("🔄 Consumer will process messages as they arrive");
        System.out.println("⏹️  Press Ctrl+C to stop the consumer");
        System.out.println();
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("🛑 Shutdown signal received...");
            System.out.println("✅ Consumer shutdown completed");
        }));

        // Keep the application running
        System.out.println("🚀 Consumer is now live and listening...");
    }

    /**
     * JMS Message Listener Component
     * Processes messages as they arrive on the queue
     */
    @Component
    static class MessageProcessor {
        
        private final AtomicLong messageCounter = new AtomicLong(0);

        @JmsListener(destination = QUEUE_NAME, containerFactory = "consumerJmsListenerContainerFactory")
        public void handleMessage(Message message, Session session) {
            long messageNumber = messageCounter.incrementAndGet();
            
            try {
                System.out.println("🔔 New message received!");
                displayMessageDetails(message, messageNumber);
                System.out.println("✅ Message #" + messageNumber + " processed successfully");
                System.out.println("👂 Listening for next message...");
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("❌ Error processing message #" + messageNumber + ": " + e.getMessage());
                e.printStackTrace();
                System.out.println("👂 Continuing to listen for next message...");
                System.out.println();
            }
        }

        private void displayMessageDetails(Message message, long messageNumber) {
            try {
                System.out.println("📨 Message #" + messageNumber + " consumed at " + LocalDateTime.now());
                System.out.println("   🆔 JMS Message ID: " + message.getJMSMessageID());
                System.out.println("   🔗 JMS Correlation ID: " + message.getJMSCorrelationID());
                System.out.println("   🏷️  JMS Type: " + message.getJMSType());
                System.out.println("   📅 JMS Timestamp: " + (message.getJMSTimestamp() > 0 ? 
                    new java.util.Date(message.getJMSTimestamp()) : "Not set"));
                System.out.println("   🎯 JMS Destination: " + message.getJMSDestination());
                
                // Display message content
                displayMessageContent(message);
                
                // Display custom properties
                displayMessageProperties(message);
                
            } catch (JMSException e) {
                System.err.println("❌ Error displaying message details: " + e.getMessage());
            }
        }

        private void displayMessageContent(Message message) throws JMSException {
            System.out.println("   📄 Message Content:");
            
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String content = textMessage.getText();
                
                if (content != null && !content.isEmpty()) {
                    // Pretty print JSON if it looks like JSON
                    if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
                        System.out.println("      📋 Type: JSON");
                        System.out.println("      📝 Content: " + content);
                    } else if (content.trim().startsWith("<") && content.trim().endsWith(">")) {
                        System.out.println("      📋 Type: XML");
                        System.out.println("      📝 Content: " + content);
                    } else {
                        System.out.println("      📋 Type: Text");
                        System.out.println("      📝 Content: " + content);
                    }
                } else {
                    System.out.println("      📝 Content: (empty)");
                }
            } else {
                System.out.println("      📋 Type: " + message.getClass().getSimpleName());
                System.out.println("      📝 Content: " + message.toString());
            }
        }

        private void displayMessageProperties(Message message) throws JMSException {
            System.out.println("   🏷️  Custom Properties:");
            
            Enumeration<?> propertyNames = message.getPropertyNames();
            boolean hasProperties = false;
            
            while (propertyNames.hasMoreElements()) {
                hasProperties = true;
                String propertyName = (String) propertyNames.nextElement();
                Object propertyValue = message.getObjectProperty(propertyName);
                System.out.println("      • " + propertyName + " = " + propertyValue);
            }
            
            if (!hasProperties) {
                System.out.println("      (no custom properties)");
            }
        }
    }

    /**
     * Independent IBM MQ Configuration using Jakarta EE for Always-Live Consumer
     * Configures JMS listeners for continuous message processing
     */
    @Configuration
    static class ConsumerIBMMQConfig {

        @Bean("consumerConnectionFactory")
        public ConnectionFactory consumerConnectionFactory() {
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
                
                System.out.println("🔧 Independent IBM MQ ConnectionFactory configured for Always-Live Consumer (Jakarta EE):");
                System.out.println("   📡 Host: localhost:1414");
                System.out.println("   🎯 Queue Manager: QM1");
                System.out.println("   📋 Channel: MY.OPEN.CHANNEL");
                System.out.println("   📦 Library: Jakarta EE");
                System.out.println("   🚫 Isolated from main application");
                System.out.println("   🌐 No web server required");
                System.out.println("   🔧 Bean name: consumerConnectionFactory");
                System.out.println("   ⏰ Configured for continuous listening");
                
                return factory;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to create IBM MQ ConnectionFactory for Always-Live Consumer", e);
            }
        }

        @Bean("consumerJmsListenerContainerFactory")
        public JmsListenerContainerFactory<?> consumerJmsListenerContainerFactory(
                @Qualifier("consumerConnectionFactory") ConnectionFactory connectionFactory) {
            
            DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            
            // Configure for optimal message processing
            factory.setConcurrency("1-3"); // 1 to 3 concurrent consumers
            factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
            factory.setReceiveTimeout(1000L); // 1 second timeout for polling
            factory.setErrorHandler(throwable -> {
                System.err.println("❌ JMS Listener Error: " + throwable.getMessage());
                throwable.printStackTrace();
                System.out.println("🔄 Listener will continue processing...");
            });
            
            System.out.println("🔧 JMS Listener Container Factory configured for Always-Live Consumer:");
            System.out.println("   🔧 Bean name: consumerJmsListenerContainerFactory");
            System.out.println("   👥 Concurrency: 1-3 consumers");
            System.out.println("   ✅ Acknowledge mode: AUTO_ACKNOWLEDGE");
            System.out.println("   ⏱️  Receive timeout: 1000ms");
            System.out.println("   🛡️  Error handling: Continue on errors");
            
            return factory;
        }
    }
} 
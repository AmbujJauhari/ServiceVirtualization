package com.service.virtualization.tibco.test;

import com.tibco.tibjms.TibjmsConnectionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import javax.jms.*;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Always-live TIBCO EMS Consumer for TEST.QUEUE.1
 * Uses direct TIBCO EMS javax.jms API without Spring abstraction
 * Completely independent from main application
 */
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
@Configuration
public class TibcoEMSStandaloneConsumer implements CommandLineRunner {

    private static final String QUEUE_NAME = "TEST.QUEUE.2";
    private final AtomicLong messageCounter = new AtomicLong(0);
    private volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("🚀 Starting Always-Live TIBCO EMS Consumer (Pure javax.jms)");
        System.out.println("📋 Source Queue: " + QUEUE_NAME);
        System.out.println("🔧 Using direct TIBCO EMS API");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 Component scanning limited to consumer only");
        System.out.println("🚫 MongoDB auto-configuration excluded");
        System.out.println("⏰ Will run continuously until stopped");
        System.out.println("=" .repeat(60));
        
        SpringApplication app = new SpringApplication(TibcoEMSStandaloneConsumer.class);
        app.setAdditionalProfiles("test-tibco-consumer-live");
        
        // Disable web environment completely since we just need to consume messages
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("✅ Always-Live Consumer initialized!");
        System.out.println("🔗 TIBCO EMS Direct API configured (javax.jms)");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 No main application beans loaded");
        System.out.println("🚫 No MongoDB components loaded");
        System.out.println("🚫 No publisher beans loaded");
        System.out.println();
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("🛑 Shutdown signal received...");
            running = false;
            System.out.println("✅ Consumer shutdown completed");
        }));

        // Start consuming messages
        startConsuming();
    }

    private void startConsuming() {
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        
        try {
            // Create TIBCO EMS ConnectionFactory
            TibjmsConnectionFactory factory = new TibjmsConnectionFactory();
            factory.setServerUrl("tcp://localhost:7222");
            factory.setUserName(null); // No authentication
            factory.setUserPassword(null); // No authentication
            
            System.out.println("🔧 TIBCO EMS Direct Connection configured:");
            System.out.println("   📡 Server URL: tcp://localhost:7222");
            System.out.println("   🔒 Authentication: Disabled");
            System.out.println("   📦 Library: TIBCO EMS 8.6 (javax.jms)");
            System.out.println("   🚫 No Spring abstraction layer");
            System.out.println("   🔧 Direct TIBCO EMS API usage");
            System.out.println();
            
            // Create connection and session
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            consumer = session.createConsumer(queue);
            
            // Start the connection
            connection.start();
            
            System.out.println("👂 Now listening for messages on " + QUEUE_NAME + "...");
            System.out.println("📊 Message counter initialized");
            System.out.println("🔄 Consumer will process messages as they arrive");
            System.out.println("⏹️  Press Ctrl+C to stop the consumer");
            System.out.println();
            System.out.println("🚀 Consumer is now live and listening...");
            
            // Continuously listen for messages
            while (running) {
                try {
                    // Wait for message with 1 second timeout
                    Message message = consumer.receive(1000);
                    
                    if (message != null) {
                        long messageNumber = messageCounter.incrementAndGet();
                        System.out.println("🔔 New message received!");
                        displayMessageDetails(message, messageNumber);
                        System.out.println("✅ Message #" + messageNumber + " processed successfully");
                        System.out.println("👂 Listening for next message...");
                        System.out.println();
                    }
                    
                } catch (JMSException e) {
                    if (running) {
                        System.err.println("❌ Error receiving message: " + e.getMessage());
                        e.printStackTrace();
                        System.out.println("👂 Continuing to listen for next message...");
                        System.out.println();
                        
                        // Wait a bit before retrying
                        Thread.sleep(5000);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Fatal error in consumer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try {
                if (consumer != null) consumer.close();
                if (session != null) session.close();
                if (connection != null) connection.close();
                System.out.println("🔄 Resources cleaned up");
            } catch (JMSException e) {
                System.err.println("❌ Error closing resources: " + e.getMessage());
            }
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
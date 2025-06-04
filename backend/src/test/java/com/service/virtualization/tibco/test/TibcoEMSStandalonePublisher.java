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
import java.util.Scanner;

/**
 * Completely independent Spring Boot application to publish messages to TIBCO EMS
 * Uses direct TIBCO EMS javax.jms API without Spring abstraction
 * Completely independent from main application
 */
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
@Configuration
public class TibcoEMSStandalonePublisher implements CommandLineRunner {
    
    private static final String TARGET_QUEUE = "TEST.QUEUE.1";

    public static void main(String[] args) {
        System.out.println("🚀 Starting Independent TIBCO EMS Publisher (Pure javax.jms)");
        System.out.println("📋 Target Queue: " + TARGET_QUEUE);
        System.out.println("🔧 Using direct TIBCO EMS API");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 Component scanning limited to publisher only");
        System.out.println("🚫 MongoDB auto-configuration excluded");
        System.out.println("=" .repeat(60));
        
        SpringApplication app = new SpringApplication(TibcoEMSStandalonePublisher.class);
        app.setAdditionalProfiles("test-tibco-publisher");
        
        // Disable web environment completely since we just need to publish messages
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("✅ Independent Spring Boot context loaded!");
        System.out.println("🔗 TIBCO EMS Direct API configured (javax.jms)");
        System.out.println("🌐 Running without web server (no port conflicts)");
        System.out.println("🚫 No main application beans loaded");
        System.out.println("🚫 No MongoDB components loaded");
        System.out.println("🎯 Dynamic destination creation enabled");
        System.out.println();

        // Interactive message publishing
        publishMessages();
        
        System.exit(0);
    }

    private void publishMessages() {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        
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
            System.out.println("   🎯 Dynamic destination creation enabled");
            System.out.println();
            
            // Create connection and session
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Attempt dynamic queue creation
            System.out.println("🔧 Attempting dynamic queue creation...");
            if (createQueueDynamically(session, TARGET_QUEUE)) {
                System.out.println("✅ Queue created or verified: " + TARGET_QUEUE);
            } else {
                System.out.println("⚠️  Queue creation failed, will attempt to use existing queue");
            }
            
            // Create queue reference and producer
            Queue queue = session.createQueue(TARGET_QUEUE);
            producer = session.createProducer(queue);
            
            // Start the connection
            connection.start();
            
            System.out.println("🚀 TIBCO EMS Message Publisher is ready!");
            System.out.println("📤 Publishing to queue: " + TARGET_QUEUE);
            System.out.println("📝 Enter messages to publish (type 'quit' to exit):");
            System.out.println();
            
            // Interactive publishing
            Scanner scanner = new Scanner(System.in);
            int messageCount = 0;
            
            while (true) {
                System.out.print("📝 Enter message (or 'quit' to exit): ");
                String input = scanner.nextLine().trim();
                
                if ("quit".equalsIgnoreCase(input)) {
                    System.out.println("👋 Goodbye! Published " + messageCount + " messages.");
                    break;
                }
                
                if (input.isEmpty()) {
                    System.out.println("⚠️  Empty message, please enter some content.");
                    continue;
                }
                
                try {
                    // Publish the message
                    publishMessage(session, producer, input, ++messageCount);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error publishing message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("❌ Fatal error in publisher: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try {
                if (producer != null) producer.close();
                if (session != null) session.close();
                if (connection != null) connection.close();
                System.out.println("🔄 Resources cleaned up");
            } catch (JMSException e) {
                System.err.println("❌ Error closing resources: " + e.getMessage());
            }
        }
    }

    /**
     * Attempts to create a queue dynamically using JMS API
     * This works when TIBCO EMS server is configured with create_destination=enabled
     */
    private boolean createQueueDynamically(Session session, String queueName) {
        try {
            System.out.println("   🎯 Creating queue: " + queueName);
            
            // Create the queue reference
            Queue queue = session.createQueue(queueName);
            
            // Try to create a temporary producer to verify/create the queue
            MessageProducer tempProducer = session.createProducer(queue);
            
            // Create a test message to ensure the queue is created
            TextMessage testMessage = session.createTextMessage("QUEUE_CREATION_TEST");
            testMessage.setStringProperty("MessageType", "QueueCreationTest");
            testMessage.setStringProperty("CreatedBy", "TibcoEMSStandalonePublisher");
            testMessage.setStringProperty("CreatedAt", LocalDateTime.now().toString());
            
            // Send the test message (this forces queue creation)
            tempProducer.send(testMessage);
            
            // Immediately consume the test message to clean up
            MessageConsumer tempConsumer = session.createConsumer(queue);
            Message receivedTest = tempConsumer.receive(1000); // 1 second timeout
            
            if (receivedTest != null) {
                System.out.println("   ✅ Queue creation verified with test message");
            } else {
                System.out.println("   ⚠️  Queue created but test message not received");
            }
            
            // Clean up temporary resources
            tempProducer.close();
            tempConsumer.close();
            
            System.out.println("   📋 Queue ready: " + queueName);
            return true;
            
        } catch (JMSException e) {
            System.err.println("   ❌ Dynamic queue creation failed: " + e.getMessage());
            System.err.println("   💡 Make sure TIBCO EMS server has create_destination=enabled");
            return false;
        }
    }

    private void publishMessage(Session session, MessageProducer producer, String messageContent, int messageNumber) {
        try {
            System.out.println();
            System.out.println("📤 Publishing message #" + messageNumber + " at " + LocalDateTime.now());
            
            // Determine message type and create appropriate content
            String finalContent;
            String contentType;
            
            if (messageContent.trim().startsWith("{") && messageContent.trim().endsWith("}")) {
                // JSON message
                finalContent = messageContent;
                contentType = "JSON";
            } else if (messageContent.trim().startsWith("<") && messageContent.trim().endsWith(">")) {
                // XML message
                finalContent = messageContent;
                contentType = "XML";
            } else {
                // Plain text message
                finalContent = messageContent;
                contentType = "Text";
            }
            
            // Create and send the message
            TextMessage textMessage = session.createTextMessage(finalContent);
            
            // Add message properties
            textMessage.setStringProperty("MessageType", contentType);
            textMessage.setStringProperty("Publisher", "TibcoEMSStandalonePublisher");
            textMessage.setStringProperty("MessageNumber", String.valueOf(messageNumber));
            textMessage.setStringProperty("Timestamp", LocalDateTime.now().toString());
            
            // Send the message
            producer.send(textMessage);
            
            System.out.println("   📋 Content Type: " + contentType);
            System.out.println("   📝 Content: " + finalContent);
            System.out.println("   🎯 Queue: " + TARGET_QUEUE);
            System.out.println("   ✅ Message published successfully!");
            System.out.println();
            
        } catch (JMSException e) {
            System.err.println("❌ Failed to publish message: " + e.getMessage());
            throw new RuntimeException("Message publishing failed", e);
        }
    }
} 
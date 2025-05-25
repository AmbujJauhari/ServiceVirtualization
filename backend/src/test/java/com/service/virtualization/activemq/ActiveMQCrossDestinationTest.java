package com.service.virtualization.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;

/**
 * Test class demonstrating cross-destination functionality.
 * Shows how to publish to one destination (queue) and receive response from another (topic).
 */
public class ActiveMQCrossDestinationTest {
    
    /**
     * Test sending to a queue and receiving response from a topic.
     */
    @Test
    public void testQueueToTopicResponse() throws Exception {
        String brokerUrl = "tcp://localhost:61616";
        String requestQueueName = "request-queue";
        String responseTopicName = "response-topic";
        String messageContent = "Test message for queue to topic response";

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("admin", "admin", brokerUrl);
        
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Create request queue and response topic
            Queue requestQueue = session.createQueue(requestQueueName);
            Topic responseTopic = session.createTopic(responseTopicName);
            
            // Set up consumer for response topic BEFORE sending message
            MessageConsumer responseConsumer = session.createConsumer(responseTopic);
            
            // Send message to request queue
            MessageProducer requestProducer = session.createProducer(requestQueue);
            TextMessage requestMessage = session.createTextMessage(messageContent);
            requestProducer.send(requestMessage);
            System.out.println("Sent message to queue: " + messageContent);
            
            // Wait for response on topic
            System.out.println("Waiting for response on topic...");
            TextMessage responseMessage = (TextMessage) responseConsumer.receive(10000); // wait up to 10 seconds
            
            if (responseMessage != null) {
                System.out.println("Received response from topic: " + responseMessage.getText());
            } else {
                System.out.println("No response received from topic");
            }
            
            session.close();
        }
    }
    
    /**
     * Test sending to a topic and receiving response from a queue.
     */
    @Test
    public void testTopicToQueueResponse() throws Exception {
        String brokerUrl = "tcp://localhost:61616";
        String requestTopicName = "request-topic";
        String responseQueueName = "response-queue";
        String messageContent = "Test message for topic to queue response";

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("admin", "admin", brokerUrl);
        
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Create request topic and response queue
            Topic requestTopic = session.createTopic(requestTopicName);
            Queue responseQueue = session.createQueue(responseQueueName);
            
            // Set up consumer for response queue
            MessageConsumer responseConsumer = session.createConsumer(responseQueue);
            
            // Send message to request topic
            MessageProducer requestProducer = session.createProducer(requestTopic);
            TextMessage requestMessage = session.createTextMessage(messageContent);
            requestProducer.send(requestMessage);
            System.out.println("Sent message to topic: " + messageContent);
            
            // Wait for response on queue
            System.out.println("Waiting for response on queue...");
            TextMessage responseMessage = (TextMessage) responseConsumer.receive(10000); // wait up to 10 seconds
            
            if (responseMessage != null) {
                System.out.println("Received response from queue: " + responseMessage.getText());
            } else {
                System.out.println("No response received from queue");
            }
            
            session.close();
        }
    }
} 
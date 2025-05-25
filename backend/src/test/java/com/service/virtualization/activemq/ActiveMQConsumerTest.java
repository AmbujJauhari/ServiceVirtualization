package com.service.virtualization.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

public class ActiveMQConsumerTest {
    @Test
    public void receiveTextMessage() throws Exception {
        String brokerUrl = "tcp://localhost:61616";
        String queueName = "test-queue";

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("admin", "admin", brokerUrl);
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            System.out.println("Waiting for message...");
            TextMessage message = (TextMessage) consumer.receive(5000); // wait up to 5 seconds
            if (message != null) {
                System.out.println("Received message: " + message.getText());
            } else {
                System.out.println("No message received");
            }
            session.close();
        }
    }
} 
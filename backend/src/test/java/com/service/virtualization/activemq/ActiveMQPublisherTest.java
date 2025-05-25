package com.service.virtualization.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import jakarta.jms.Connection;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

public class ActiveMQPublisherTest {
    @Test
    public void sendTextMessage() throws Exception {
        String brokerUrl = "tcp://localhost:61616";
        String queueName = "request.test";
        String messageContent = "Hello from publisher test!";

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("admin", "admin", brokerUrl);
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage(messageContent);
            producer.send(message);
            System.out.println("Message sent: " + messageContent);
            session.close();
        }
    }
} 
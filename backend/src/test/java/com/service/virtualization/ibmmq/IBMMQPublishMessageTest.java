package com.service.virtualization.ibmmq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration test for publishing messages to IBM MQ
 */
@SpringBootTest
//@ActiveProfiles("test")
public class IBMMQPublishMessageTest {

    @Autowired
    @Qualifier("ibmmqQueueJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Test
    public void testPublishMessageToTestQueue1() {
        // Test message content
        String testMessage = """
            {
                "messageId": "test-001",
                "timestamp": "2025-06-04T10:30:00Z",
                "content": "This is a test message for IBM MQ service virtualization",
                "priority": "HIGH",
                "sender": "ServiceVirtualization-Test"
            }
            """;

        // Publish message to TEST.QUEUE.1
        assertDoesNotThrow(() -> {
            jmsTemplate.send("TEST.QUEUE.1", session -> {
                var message = session.createTextMessage(testMessage);
                message.setStringProperty("MessageType", "TEST");
                message.setStringProperty("Source", "IntegrationTest");
                message.setJMSCorrelationID("TEST-CORRELATION-001");
                return message;
            });
            
            System.out.println("✅ Successfully published message to TEST.QUEUE.1");
            System.out.println("📄 Message content: " + testMessage);
        });
    }

    @Test
    public void testPublishMultipleMessages() {
        // Test publishing multiple messages
        assertDoesNotThrow(() -> {
            for (int i = 1; i <= 3; i++) {
                final int messageNumber = i;
                String message = "Test message #" + messageNumber + " - Service Virtualization Test";
                
                jmsTemplate.send("TEST.QUEUE.1", session -> {
                    var jmsMessage = session.createTextMessage(message);
                    jmsMessage.setStringProperty("MessageNumber", String.valueOf(messageNumber));
                    jmsMessage.setStringProperty("TestBatch", "MultipleMessages");
                    return jmsMessage;
                });
                
                System.out.println("📤 Published message " + messageNumber + ": " + message);
            }
            
            System.out.println("✅ Successfully published 3 messages to TEST.QUEUE.1");
        });
    }

    @Test
    public void testPublishXMLMessage() {
        // Test XML message
        String xmlMessage = """
            <?xml version="1.0" encoding="UTF-8"?>
            <order>
                <orderId>ORDER-12345</orderId>
                <customerId>CUST-001</customerId>
                <items>
                    <item>
                        <productId>PROD-ABC</productId>
                        <quantity>2</quantity>
                        <price>29.99</price>
                    </item>
                </items>
                <total>59.98</total>
            </order>
            """;

        assertDoesNotThrow(() -> {
            jmsTemplate.send("TEST.QUEUE.1", session -> {
                var message = session.createTextMessage(xmlMessage);
                message.setStringProperty("ContentType", "application/xml");
                message.setStringProperty("MessageFormat", "XML");
                message.setJMSCorrelationID("ORDER-12345");
                return message;
            });
            
            System.out.println("✅ Successfully published XML message to TEST.QUEUE.1");
            System.out.println("📄 XML content: " + xmlMessage);
        });
    }
} 
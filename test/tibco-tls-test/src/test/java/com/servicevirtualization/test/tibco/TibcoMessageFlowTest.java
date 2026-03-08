package com.servicevirtualization.test.tibco;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end message flow tests for Tibco EMS: send a message, receive it back.
 *
 * These tests prove that:
 *   1. TLS connection is established (prerequisite)
 *   2. EMS queues and topics are accessible
 *   3. Messages can be sent and received through the Istio TLS passthrough
 *   4. Message content is preserved (no corruption through the Istio proxy)
 *   5. Server A and Server B are isolated — messages from A are not visible on B
 */
@DisplayName("Tibco EMS Message Flow Tests")
class TibcoMessageFlowTest {

    private static final Logger log = LoggerFactory.getLogger(TibcoMessageFlowTest.class);

    // -------------------------------------------------------------------------
    // Queue-based flow (point-to-point)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Margin Server A: send and receive queue message via TLS")
    void marginServerASendAndReceiveQueueMessage() throws JMSException {
        log.info("=== TEST: Margin Tibco Server A queue send/receive ===");

        String correlationId = "margin-svra-test-" + UUID.randomUUID();
        String payload = "{ \"server\": \"serverA\", \"team\": \"margin\", \"correlationId\": \"" + correlationId + "\", \"test\": true }";

        try (Connection conn = TibcoConnectionHelper.createMarginServerAConnection()) {
            conn.start();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(TibcoTestConfig.getMarginServerAQueue());

            // SEND
            MessageProducer producer = session.createProducer(queue);
            TextMessage outboundMsg = session.createTextMessage(payload);
            outboundMsg.setJMSCorrelationID(correlationId);
            producer.send(outboundMsg);
            log.info("SENT message to Server A queue: correlationId={}", correlationId);

            // RECEIVE — synchronous with timeout, filtered by correlation ID
            MessageConsumer consumer = session.createConsumer(queue,
                    "JMSCorrelationID = '" + correlationId + "'");
            Message inboundMsg = consumer.receive(TibcoTestConfig.getMessageTimeout());

            assertNotNull(inboundMsg, "Message should be received from Server A queue within timeout");
            assertInstanceOf(TextMessage.class, inboundMsg);

            String receivedText = ((TextMessage) inboundMsg).getText();
            assertEquals(payload, receivedText, "Received message content must match sent content");
            log.info("RECEIVED message from Server A queue: content matches — queue flow confirmed");

            session.close();
        }
    }

    @Test
    @DisplayName("Margin Server B: send and receive queue message via TLS")
    void marginServerBSendAndReceiveQueueMessage() throws JMSException {
        log.info("=== TEST: Margin Tibco Server B queue send/receive ===");

        String correlationId = "margin-svrb-test-" + UUID.randomUUID();
        String payload = "{ \"server\": \"serverB\", \"team\": \"margin\", \"correlationId\": \"" + correlationId + "\", \"test\": true }";

        try (Connection conn = TibcoConnectionHelper.createMarginServerBConnection()) {
            conn.start();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(TibcoTestConfig.getMarginServerBQueue());

            // SEND
            MessageProducer producer = session.createProducer(queue);
            TextMessage outboundMsg = session.createTextMessage(payload);
            outboundMsg.setJMSCorrelationID(correlationId);
            producer.send(outboundMsg);
            log.info("SENT message to Server B queue: correlationId={}", correlationId);

            // RECEIVE
            MessageConsumer consumer = session.createConsumer(queue,
                    "JMSCorrelationID = '" + correlationId + "'");
            Message inboundMsg = consumer.receive(TibcoTestConfig.getMessageTimeout());

            assertNotNull(inboundMsg, "Message should be received from Server B queue within timeout");
            assertInstanceOf(TextMessage.class, inboundMsg);

            String receivedText = ((TextMessage) inboundMsg).getText();
            assertEquals(payload, receivedText, "Received message content must match sent content");
            log.info("RECEIVED message from Server B queue: content matches — queue flow confirmed");

            session.close();
        }
    }

    // -------------------------------------------------------------------------
    // Topic-based flow (publish-subscribe)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Margin Server A: publish and subscribe to topic via TLS")
    void marginServerAPublishSubscribeTopic() throws JMSException {
        log.info("=== TEST: Margin Tibco Server A topic publish/subscribe ===");

        String correlationId = "margin-svra-topic-" + UUID.randomUUID();
        String payload = "{ \"server\": \"serverA\", \"topic\": \"test\", \"correlationId\": \"" + correlationId + "\" }";

        try (Connection conn = TibcoConnectionHelper.createMarginServerAConnection()) {
            conn.start();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(TibcoTestConfig.getMarginServerATopic());

            // Subscribe BEFORE publish to avoid missing the message
            MessageConsumer subscriber = session.createConsumer(topic,
                    "JMSCorrelationID = '" + correlationId + "'");

            // PUBLISH
            MessageProducer publisher = session.createProducer(topic);
            TextMessage outboundMsg = session.createTextMessage(payload);
            outboundMsg.setJMSCorrelationID(correlationId);
            publisher.send(outboundMsg);
            log.info("PUBLISHED message to Server A topic: correlationId={}", correlationId);

            // RECEIVE
            Message inboundMsg = subscriber.receive(TibcoTestConfig.getMessageTimeout());

            assertNotNull(inboundMsg, "Message should be received from Server A topic within timeout");
            assertInstanceOf(TextMessage.class, inboundMsg);

            String receivedText = ((TextMessage) inboundMsg).getText();
            assertEquals(payload, receivedText, "Received topic message content must match published content");
            log.info("RECEIVED topic message from Server A: content matches — pub/sub flow confirmed");

            session.close();
        }
    }

    // -------------------------------------------------------------------------
    // Isolation test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Instance isolation: Server A message not visible on Server B queue")
    void serverAMessageNotVisibleOnServerBQueue() throws JMSException {
        log.info("=== TEST: Instance isolation — Server A messages stay on Server A ===");

        String correlationId = "isolation-test-" + UUID.randomUUID();
        String payload = "{ \"server\": \"serverA\", \"correlationId\": \"" + correlationId + "\" }";

        // Step 1: Send a message to Server A's queue
        try (Connection serverAConn = TibcoConnectionHelper.createMarginServerAConnection()) {
            serverAConn.start();
            Session session = serverAConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(TibcoTestConfig.getMarginServerAQueue());

            MessageProducer producer = session.createProducer(queue);
            TextMessage msg = session.createTextMessage(payload);
            msg.setJMSCorrelationID(correlationId);
            producer.send(msg);
            log.info("SENT message to Server A queue with correlationId={}", correlationId);
            session.close();
        }

        // Step 2: Verify it is NOT visible on Server B's queue (different EMS pod)
        try (Connection serverBConn = TibcoConnectionHelper.createMarginServerBConnection()) {
            serverBConn.start();
            Session session = serverBConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(TibcoTestConfig.getMarginServerBQueue());

            MessageConsumer consumer = session.createConsumer(queue,
                    "JMSCorrelationID = '" + correlationId + "'");

            // Short timeout — message should not be there
            Message msg = consumer.receive(2000);
            assertNull(msg,
                    "Message sent to Server A must NOT be visible on Server B. " +
                    "If this fails, the two EMS instances are not properly isolated via SNI routing.");

            log.info("Isolation confirmed: Server A message not visible on Server B");
            session.close();
        }
    }
}

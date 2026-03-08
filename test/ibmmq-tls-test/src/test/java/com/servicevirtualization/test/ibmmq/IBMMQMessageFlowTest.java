package com.servicevirtualization.test.ibmmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end message flow tests: put a message on a queue, get it back.
 *
 * These tests prove that:
 *   1. TLS connection is established (prerequisite)
 *   2. MQ queues are accessible
 *   3. Messages can be sent and received through the Istio TLS passthrough
 *   4. Message content is preserved (no corruption through the Istio proxy)
 *   5. Margin messages go only to the margin queue; collateral only to collateral
 */
@DisplayName("IBM MQ Message Flow Tests")
class IBMMQMessageFlowTest {

    private static final Logger log = LoggerFactory.getLogger(IBMMQMessageFlowTest.class);

    @Test
    @DisplayName("Margin MQ: put and get text message via TLS")
    void marginMQPutAndGetMessage() throws JMSException {
        log.info("=== TEST: Margin MQ put/get message ===");

        String correlationId = "margin-test-" + UUID.randomUUID();
        String payload = "{ \"team\": \"margin\", \"correlationId\": \"" + correlationId + "\", \"test\": true }";

        try (Connection conn = IBMMQConnectionHelper.createMarginConnection()) {
            conn.start();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(IBMMQTestConfig.getMarginQueue());

            // PUT
            MessageProducer producer = session.createProducer(queue);
            TextMessage outboundMsg = session.createTextMessage(payload);
            outboundMsg.setJMSCorrelationID(correlationId);
            producer.send(outboundMsg);
            log.info("PUT message to margin queue: correlationId={}", correlationId);

            // GET — synchronous receive with timeout
            MessageConsumer consumer = session.createConsumer(queue,
                    "JMSCorrelationID = '" + correlationId + "'");
            Message inboundMsg = consumer.receive(IBMMQTestConfig.getMessageTimeout());

            assertNotNull(inboundMsg, "Message should be received from margin queue within timeout");
            assertInstanceOf(TextMessage.class, inboundMsg);

            String receivedText = ((TextMessage) inboundMsg).getText();
            assertEquals(payload, receivedText, "Received message content must match sent content");
            log.info("GET message from margin queue: content matches — message flow confirmed");

            session.close();
        }
    }

    @Test
    @DisplayName("Collateral MQ: put and get text message via TLS")
    void collateralMQPutAndGetMessage() throws JMSException {
        log.info("=== TEST: Collateral MQ put/get message ===");

        String correlationId = "collateral-test-" + UUID.randomUUID();
        String payload = "{ \"team\": \"collateral\", \"correlationId\": \"" + correlationId + "\", \"test\": true }";

        try (Connection conn = IBMMQConnectionHelper.createCollateralConnection()) {
            conn.start();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(IBMMQTestConfig.getCollateralQueue());

            // PUT
            MessageProducer producer = session.createProducer(queue);
            TextMessage outboundMsg = session.createTextMessage(payload);
            outboundMsg.setJMSCorrelationID(correlationId);
            producer.send(outboundMsg);
            log.info("PUT message to collateral queue: correlationId={}", correlationId);

            // GET
            MessageConsumer consumer = session.createConsumer(queue,
                    "JMSCorrelationID = '" + correlationId + "'");
            Message inboundMsg = consumer.receive(IBMMQTestConfig.getMessageTimeout());

            assertNotNull(inboundMsg, "Message should be received from collateral queue within timeout");
            assertInstanceOf(TextMessage.class, inboundMsg);

            String receivedText = ((TextMessage) inboundMsg).getText();
            assertEquals(payload, receivedText, "Received message content must match sent content");
            log.info("GET message from collateral queue: content matches — message flow confirmed");

            session.close();
        }
    }

    @Test
    @DisplayName("Namespace isolation: margin message not visible on collateral queue")
    void marginMessageNotVisibleOnCollateralQueue() throws JMSException {
        log.info("=== TEST: Namespace isolation — margin messages stay in margin ===");

        String correlationId = "isolation-test-" + UUID.randomUUID();
        String payload = "{ \"team\": \"margin\", \"correlationId\": \"" + correlationId + "\" }";

        // Step 1: Put a message on margin's queue
        try (Connection marginConn = IBMMQConnectionHelper.createMarginConnection()) {
            marginConn.start();
            Session session = marginConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(IBMMQTestConfig.getMarginQueue());

            MessageProducer producer = session.createProducer(queue);
            TextMessage msg = session.createTextMessage(payload);
            msg.setJMSCorrelationID(correlationId);
            producer.send(msg);
            log.info("PUT message on margin queue with correlationId={}", correlationId);
            session.close();
        }

        // Step 2: Verify it is NOT visible on collateral's queue (different MQ pod, different namespace)
        try (Connection collateralConn = IBMMQConnectionHelper.createCollateralConnection()) {
            collateralConn.start();
            Session session = collateralConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(IBMMQTestConfig.getCollateralQueue());

            MessageConsumer consumer = session.createConsumer(queue,
                    "JMSCorrelationID = '" + correlationId + "'");

            // Short timeout — message should not be there
            Message msg = consumer.receive(2000);
            assertNull(msg,
                    "Message put on margin queue must NOT be visible on collateral queue. " +
                    "If this fails, the two MQ instances are not properly isolated.");

            log.info("Isolation confirmed: margin message not visible on collateral queue");
            session.close();
        }
    }
}

package com.service.virtualization.jmshelper.service;

import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import com.service.virtualization.jmshelper.model.ConsumeRequest;
import com.service.virtualization.jmshelper.model.ConsumeResponse;
import com.service.virtualization.jmshelper.model.PublishRequest;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes and consumes JMS messages on IBM MQ using the jakarta.jms API.
 *
 * A fresh JMS connection is created for each operation — this is intentional
 * for a test helper where simplicity and isolation matter more than throughput.
 */
@Service
public class IbmmqJmsService {

    private static final Logger log = LoggerFactory.getLogger(IbmmqJmsService.class);

    @Value("${jms-helper.ibmmq.host:localhost}")
    private String host;

    @Value("${jms-helper.ibmmq.port:1414}")
    private int port;

    @Value("${jms-helper.ibmmq.queue-manager:QM1}")
    private String queueManager;

    @Value("${jms-helper.ibmmq.channel:DEV.APP.SVRCONN}")
    private String channel;

    @Value("${jms-helper.ibmmq.username:app}")
    private String username;

    @Value("${jms-helper.ibmmq.password:passw0rd}")
    private String password;

    // ── Publish ──────────────────────────────────────────────────────────────

    public void publish(PublishRequest req) throws JMSException {
        log.info("IBM MQ publish → {} {} | body length={}", req.destinationType(), req.destinationName(),
                req.message() != null ? req.message().length() : 0);

        try (Connection conn = createConnectionFactory().createConnection(username, password)) {
            conn.start();
            try (Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Destination dest = destination(session, req.destinationType(), req.destinationName());
                MessageProducer producer = session.createProducer(dest);

                TextMessage msg = session.createTextMessage(req.message());

                if (req.correlationId() != null) {
                    msg.setJMSCorrelationID(req.correlationId());
                }
                if (req.replyTo() != null) {
                    Destination replyDest = destination(session, req.destinationType(), req.replyTo());
                    msg.setJMSReplyTo(replyDest);
                }
                if (req.properties() != null) {
                    for (Map.Entry<String, String> e : req.properties().entrySet()) {
                        msg.setStringProperty(e.getKey(), e.getValue());
                    }
                }

                producer.send(msg);
                log.info("IBM MQ message published to {}", req.destinationName());
            }
        }
    }

    // ── Consume ──────────────────────────────────────────────────────────────

    public ConsumeResponse consume(ConsumeRequest req) throws JMSException {
        log.info("IBM MQ consume ← {} {} | timeout={}ms", req.destinationType(), req.destinationName(),
                req.effectiveTimeout());

        try (Connection conn = createConnectionFactory().createConnection(username, password)) {
            conn.start();
            try (Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Destination dest = destination(session, req.destinationType(), req.destinationName());
                MessageConsumer consumer = req.selector() != null
                        ? session.createConsumer(dest, req.selector())
                        : session.createConsumer(dest);

                Message received = consumer.receive(req.effectiveTimeout());

                if (received == null) {
                    log.info("IBM MQ consume timed out — no message on {}", req.destinationName());
                    return ConsumeResponse.notFound();
                }

                String body = received instanceof TextMessage
                        ? ((TextMessage) received).getText()
                        : received.toString();

                String correlationId = received.getJMSCorrelationID();
                Map<String, String> props = extractStringProperties(received);

                log.info("IBM MQ message received from {} | correlationId={}", req.destinationName(), correlationId);
                return new ConsumeResponse(true, body, correlationId, props);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ConnectionFactory createConnectionFactory() throws JMSException {
        MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
        factory.setHostName(host);
        factory.setPort(port);
        factory.setQueueManager(queueManager);
        factory.setChannel(channel);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208);
        return factory;
    }

    private Destination destination(Session session, String type, String name) throws JMSException {
        return "TOPIC".equalsIgnoreCase(type)
                ? session.createTopic(name)
                : session.createQueue(name);
    }

    private Map<String, String> extractStringProperties(Message msg) {
        Map<String, String> result = new HashMap<>();
        try {
            var names = msg.getPropertyNames();
            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();
                try {
                    result.put(key, msg.getStringProperty(key));
                } catch (JMSException ignored) {
                    // non-string property — skip
                }
            }
        } catch (JMSException ignored) {
            // best-effort
        }
        return result;
    }
}

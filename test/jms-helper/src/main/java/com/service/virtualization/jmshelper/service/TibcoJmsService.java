package com.service.virtualization.jmshelper.service;

import com.service.virtualization.jmshelper.model.ConsumeRequest;
import com.service.virtualization.jmshelper.model.ConsumeResponse;
import com.service.virtualization.jmshelper.model.PublishRequest;
import com.tibco.tibjms.TibjmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes and consumes JMS messages on Tibco EMS using the javax.jms API.
 *
 * Tibco EMS 8.6 ships a javax.jms client (not jakarta), so this service uses
 * {@code javax.jms.*} directly — no jakarta adapter is needed here because we
 * own both the producer and the consumer.
 *
 * A fresh JMS connection is created per operation for simplicity and test isolation.
 */
@Service
public class TibcoJmsService {

    private static final Logger log = LoggerFactory.getLogger(TibcoJmsService.class);

    @Value("${jms-helper.tibco.url:tcp://localhost:7222}")
    private String serverUrl;

    @Value("${jms-helper.tibco.username:admin}")
    private String username;

    @Value("${jms-helper.tibco.password:admin}")
    private String password;

    // ── Publish ──────────────────────────────────────────────────────────────

    public void publish(PublishRequest req) throws JMSException {
        log.info("Tibco publish → {} {} | body length={}", req.destinationType(), req.destinationName(),
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
                log.info("Tibco message published to {}", req.destinationName());
            }
        }
    }

    // ── Consume ──────────────────────────────────────────────────────────────

    public ConsumeResponse consume(ConsumeRequest req) throws JMSException {
        log.info("Tibco consume ← {} {} | timeout={}ms", req.destinationType(), req.destinationName(),
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
                    log.info("Tibco consume timed out — no message on {}", req.destinationName());
                    return ConsumeResponse.notFound();
                }

                String body = received instanceof TextMessage
                        ? ((TextMessage) received).getText()
                        : received.toString();

                String correlationId = received.getJMSCorrelationID();
                Map<String, String> props = extractStringProperties(received);

                log.info("Tibco message received from {} | correlationId={}", req.destinationName(), correlationId);
                return new ConsumeResponse(true, body, correlationId, props);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ConnectionFactory createConnectionFactory() throws JMSException {
        TibjmsConnectionFactory factory = new TibjmsConnectionFactory();
        factory.setServerUrl(serverUrl);
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

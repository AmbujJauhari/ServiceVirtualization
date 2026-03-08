package com.service.virtualization.jmshelper.model;

import java.util.Map;

/**
 * Request body for POST /ibmmq/publish and POST /tibco/publish.
 *
 * @param destinationType  "QUEUE" or "TOPIC"
 * @param destinationName  queue or topic name
 * @param message          text body of the JMS message
 * @param correlationId    optional JMSCorrelationID to set on the outgoing message
 * @param replyTo          optional reply-to destination name (same type as destinationType)
 * @param properties       optional map of string JMS properties to set on the message
 */
public record PublishRequest(
        String destinationType,
        String destinationName,
        String message,
        String correlationId,
        String replyTo,
        Map<String, String> properties
) {}

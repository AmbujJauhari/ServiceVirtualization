package com.service.virtualization.jmshelper.model;

import java.util.Map;

/**
 * Response body returned by POST /ibmmq/consume and POST /tibco/consume.
 *
 * @param found          true if a message was received before the timeout
 * @param message        text body of the received message (null when found=false)
 * @param correlationId  JMSCorrelationID of the received message (may be null)
 * @param properties     JMS string properties on the received message
 */
public record ConsumeResponse(
        boolean found,
        String message,
        String correlationId,
        Map<String, String> properties
) {
    public static ConsumeResponse notFound() {
        return new ConsumeResponse(false, null, null, Map.of());
    }
}

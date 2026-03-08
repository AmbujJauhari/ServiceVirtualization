package com.service.virtualization.jmshelper.model;

/**
 * Request body for POST /ibmmq/consume and POST /tibco/consume.
 *
 * The endpoint blocks until a message arrives on the destination or the timeout
 * elapses (whichever comes first).
 *
 * @param destinationType  "QUEUE" or "TOPIC"
 * @param destinationName  queue or topic name
 * @param timeoutMs        how long to wait for a message (default 5000 ms)
 * @param selector         optional JMS message selector expression
 */
public record ConsumeRequest(
        String destinationType,
        String destinationName,
        Long timeoutMs,
        String selector
) {
    public long effectiveTimeout() {
        return timeoutMs != null ? timeoutMs : 5_000L;
    }
}

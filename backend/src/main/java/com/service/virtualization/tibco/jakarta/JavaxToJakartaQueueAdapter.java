package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

/**
 * Jakarta Queue adapter
 */
public class JavaxToJakartaQueueAdapter implements jakarta.jms.Queue {
    final javax.jms.Queue javaxQueue; // package-private for session access

    public JavaxToJakartaQueueAdapter(javax.jms.Queue javaxQueue) {
        this.javaxQueue = javaxQueue;
    }

    @Override
    public String getQueueName() throws JMSException {
        try {
            return javaxQueue.getQueueName();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }
}
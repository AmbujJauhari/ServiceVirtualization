package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

/**
 * Jakarta Topic adapter
 */
public class JavaxToJakartaTopicAdapter implements jakarta.jms.Topic {
    final javax.jms.Topic javaxTopic; // package-private for session access

    public JavaxToJakartaTopicAdapter(javax.jms.Topic javaxTopic) {
        this.javaxTopic = javaxTopic;
    }

    @Override
    public String getTopicName() throws JMSException {
        try {
            return javaxTopic.getTopicName();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }
}
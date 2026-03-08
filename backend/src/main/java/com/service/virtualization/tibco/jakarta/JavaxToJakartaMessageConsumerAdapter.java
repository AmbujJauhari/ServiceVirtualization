package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

/**
 * Jakarta MessageConsumer adapter
 */
public class JavaxToJakartaMessageConsumerAdapter implements jakarta.jms.MessageConsumer {
    private final javax.jms.MessageConsumer javaxConsumer;

    public JavaxToJakartaMessageConsumerAdapter(javax.jms.MessageConsumer javaxConsumer) {
        this.javaxConsumer = javaxConsumer;
    }

    @Override
    public String getMessageSelector() throws JMSException {
        try {
            return javaxConsumer.getMessageSelector();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.MessageListener getMessageListener() throws JMSException {
        throw new UnsupportedOperationException("MessageListener not supported in adapter");
    }

    @Override
    public void setMessageListener(jakarta.jms.MessageListener listener) throws JMSException {
        throw new UnsupportedOperationException("MessageListener not supported in adapter");
    }

    @Override
    public jakarta.jms.Message receive() throws JMSException {
        try {
            javax.jms.Message javaxMessage = javaxConsumer.receive();
            if (javaxMessage == null) {
                return null;
            }
            return new JavaxToJakartaMessageAdapter(javaxMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.Message receive(long timeout) throws JMSException {
        try {
            javax.jms.Message javaxMessage = javaxConsumer.receive(timeout);
            if (javaxMessage == null) {
                return null;
            }
            return new JavaxToJakartaMessageAdapter(javaxMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.Message receiveNoWait() throws JMSException {
        try {
            javax.jms.Message javaxMessage = javaxConsumer.receiveNoWait();
            if (javaxMessage == null) {
                return null;
            }
            return new JavaxToJakartaMessageAdapter(javaxMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void close() throws JMSException {
        try {
            javaxConsumer.close();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }
}
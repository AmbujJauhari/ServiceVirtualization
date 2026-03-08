package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

/**
 * Jakarta MessageProducer adapter
 */
public class JavaxToJakartaMessageProducerAdapter implements jakarta.jms.MessageProducer {
    private final javax.jms.MessageProducer javaxProducer;

    public JavaxToJakartaMessageProducerAdapter(javax.jms.MessageProducer javaxProducer) {
        this.javaxProducer = javaxProducer;
    }

    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        try {
            javaxProducer.setDisableMessageID(value);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public boolean getDisableMessageID() throws JMSException {
        try {
            return javaxProducer.getDisableMessageID();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        try {
            javaxProducer.setDisableMessageTimestamp(value);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        try {
            return javaxProducer.getDisableMessageTimestamp();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        try {
            javaxProducer.setDeliveryMode(deliveryMode);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public int getDeliveryMode() throws JMSException {
        try {
            return javaxProducer.getDeliveryMode();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void setPriority(int defaultPriority) throws JMSException {
        try {
            javaxProducer.setPriority(defaultPriority);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public int getPriority() throws JMSException {
        try {
            return javaxProducer.getPriority();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        try {
            javaxProducer.setTimeToLive(timeToLive);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public long getTimeToLive() throws JMSException {
        try {
            return javaxProducer.getTimeToLive();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void setDeliveryDelay(long deliveryDelay) throws JMSException {
        try {
            javaxProducer.setDeliveryDelay(deliveryDelay);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        try {
            return javaxProducer.getDeliveryDelay();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.Destination getDestination() throws JMSException {
        throw new UnsupportedOperationException("getDestination not supported in adapter");
    }

    @Override
    public void close() throws JMSException {
        try {
            javaxProducer.close();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void send(jakarta.jms.Message message) throws JMSException {
        try {
            javax.jms.Message javaxMessage = extractJavaxMessage(message);
            javaxProducer.send(javaxMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void send(jakarta.jms.Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        try {
            javax.jms.Message javaxMessage = extractJavaxMessage(message);
            javaxProducer.send(javaxMessage, deliveryMode, priority, timeToLive);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void send(jakarta.jms.Destination destination, jakarta.jms.Message message) throws JMSException {
        try {
            javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
            javax.jms.Message javaxMessage = extractJavaxMessage(message);
            javaxProducer.send(javaxDestination, javaxMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void send(jakarta.jms.Destination destination, jakarta.jms.Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        try {
            javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
            javax.jms.Message javaxMessage = extractJavaxMessage(message);
            javaxProducer.send(javaxDestination, javaxMessage, deliveryMode, priority, timeToLive);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void send(jakarta.jms.Message message, jakarta.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("Async send not supported in adapter");
    }

    @Override
    public void send(jakarta.jms.Message message, int deliveryMode, int priority, long timeToLive, jakarta.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("Async send not supported in adapter");
    }

    @Override
    public void send(jakarta.jms.Destination destination, jakarta.jms.Message message, jakarta.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("Async send not supported in adapter");
    }

    @Override
    public void send(jakarta.jms.Destination destination, jakarta.jms.Message message, int deliveryMode, int priority, long timeToLive, jakarta.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("Async send not supported in adapter");
    }

    /**
     * Extract the underlying javax.jms.Message from a Jakarta JMS Message
     */
    private javax.jms.Message extractJavaxMessage(jakarta.jms.Message jakartaMessage) {
        if (jakartaMessage instanceof JavaxToJakartaMessageAdapter) {
            return ((JavaxToJakartaMessageAdapter) jakartaMessage).getJavaxMessage();
        } else if (jakartaMessage instanceof JavaxToJakartaTextMessageAdapter) {
            return ((JavaxToJakartaTextMessageAdapter) jakartaMessage).getJavaxTextMessage();
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + jakartaMessage.getClass());
        }
    }

    /**
     * Extract the underlying javax.jms.Destination from a Jakarta JMS Destination
     */
    private javax.jms.Destination extractJavaxDestination(jakarta.jms.Destination jakartaDestination) {
        if (jakartaDestination instanceof JavaxToJakartaQueueAdapter) {
            return ((JavaxToJakartaQueueAdapter) jakartaDestination).javaxQueue;
        } else if (jakartaDestination instanceof JavaxToJakartaTopicAdapter) {
            return ((JavaxToJakartaTopicAdapter) jakartaDestination).javaxTopic;
        } else {
            throw new IllegalArgumentException("Unsupported destination type: " + jakartaDestination.getClass());
        }
    }
}

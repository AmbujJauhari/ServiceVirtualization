package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

public class JavaxToJakartaSessionAdapter implements jakarta.jms.Session {
    private final javax.jms.Session javaxSession;

    public JavaxToJakartaSessionAdapter(javax.jms.Session javaxSession) {
        this.javaxSession = javaxSession;
    }

    @Override
    public boolean getTransacted() throws JMSException {
        try {
            return javaxSession.getTransacted();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        try {
            return javaxSession.getAcknowledgeMode();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void commit() throws JMSException {
        try {
            javaxSession.commit();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void rollback() throws JMSException {
        try {
            javaxSession.rollback();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public void close() throws JMSException {
        try {
            javaxSession.close();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    // Message creation methods - for startup logging, these might not be needed
    @Override
    public jakarta.jms.BytesMessage createBytesMessage() throws JMSException {
        throw new UnsupportedOperationException("BytesMessage creation not supported in adapter - use JmsTemplate");
    }

    @Override
    public jakarta.jms.MapMessage createMapMessage() throws JMSException {
        throw new UnsupportedOperationException("MapMessage creation not supported in adapter - use JmsTemplate");
    }

    @Override
    public jakarta.jms.Message createMessage() throws JMSException {
        throw new UnsupportedOperationException("Message creation not supported in adapter - use JmsTemplate");
    }

    @Override
    public jakarta.jms.ObjectMessage createObjectMessage() throws JMSException {
        throw new UnsupportedOperationException("ObjectMessage creation not supported in adapter - use JmsTemplate");
    }

    @Override
    public jakarta.jms.ObjectMessage createObjectMessage(java.io.Serializable object) throws JMSException {
        throw new UnsupportedOperationException("ObjectMessage creation not supported in adapter - use JmsTemplate");
    }

    @Override
    public jakarta.jms.StreamMessage createStreamMessage() throws JMSException {
        throw new UnsupportedOperationException("StreamMessage creation not supported in adapter - use JmsTemplate");
    }

    @Override
    public jakarta.jms.TextMessage createTextMessage() throws JMSException {
        try {
            javax.jms.TextMessage javaxTextMessage = javaxSession.createTextMessage();
            return new JavaxToJakartaTextMessageAdapter(javaxTextMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.TextMessage createTextMessage(String text) throws JMSException {
        try {
            javax.jms.TextMessage javaxTextMessage = javaxSession.createTextMessage(text);
            return new JavaxToJakartaTextMessageAdapter(javaxTextMessage);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.Queue createQueue(String queueName) throws JMSException {
        try {
            javax.jms.Queue javaxQueue = javaxSession.createQueue(queueName);
            return new JavaxToJakartaQueueAdapter(javaxQueue);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.Topic createTopic(String topicName) throws JMSException {
        try {
            javax.jms.Topic javaxTopic = javaxSession.createTopic(topicName);
            return new JavaxToJakartaTopicAdapter(javaxTopic);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.TemporaryQueue createTemporaryQueue() throws JMSException {
        throw new UnsupportedOperationException("TemporaryQueue not supported");
    }

    @Override
    public jakarta.jms.TemporaryTopic createTemporaryTopic() throws JMSException {
        throw new UnsupportedOperationException("TemporaryTopic not supported");
    }

    @Override
    public void unsubscribe(String name) throws JMSException {
        throw new UnsupportedOperationException("Unsubscribe not supported");
    }

    @Override
    public jakarta.jms.MessageListener getMessageListener() throws JMSException {
        throw new UnsupportedOperationException("MessageListener not supported");
    }

    @Override
    public void setMessageListener(jakarta.jms.MessageListener listener) throws JMSException {
        throw new UnsupportedOperationException("MessageListener not supported");
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Session.run() not supported");
    }

    @Override
    public jakarta.jms.MessageProducer createProducer(jakarta.jms.Destination destination) throws JMSException {
        try {
            javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
            javax.jms.MessageProducer javaxProducer = javaxSession.createProducer(javaxDestination);
            return new JavaxToJakartaMessageProducerAdapter(javaxProducer);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.MessageConsumer createConsumer(jakarta.jms.Destination destination) throws JMSException {
        try {
            javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
            javax.jms.MessageConsumer javaxConsumer = javaxSession.createConsumer(javaxDestination);
            return new JavaxToJakartaMessageConsumerAdapter(javaxConsumer);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.MessageConsumer createConsumer(jakarta.jms.Destination destination, String messageSelector) throws JMSException {
        try {
            javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
            javax.jms.MessageConsumer javaxConsumer = javaxSession.createConsumer(javaxDestination, messageSelector);
            return new JavaxToJakartaMessageConsumerAdapter(javaxConsumer);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.MessageConsumer createConsumer(jakarta.jms.Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        try {
            javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
            javax.jms.MessageConsumer javaxConsumer = javaxSession.createConsumer(javaxDestination, messageSelector, noLocal);
            return new JavaxToJakartaMessageConsumerAdapter(javaxConsumer);
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    private javax.jms.Destination extractJavaxDestination(jakarta.jms.Destination jakartaDestination) {
        if (jakartaDestination instanceof JavaxToJakartaQueueAdapter) {
            return ((JavaxToJakartaQueueAdapter) jakartaDestination).javaxQueue;
        } else if (jakartaDestination instanceof JavaxToJakartaTopicAdapter) {
            return ((JavaxToJakartaTopicAdapter) jakartaDestination).javaxTopic;
        } else {
            throw new IllegalArgumentException("Unsupported destination type: " + jakartaDestination.getClass());
        }
    }

    @Override
    public jakarta.jms.TopicSubscriber createDurableSubscriber(jakarta.jms.Topic topic, String name) throws JMSException {
        throw new UnsupportedOperationException("DurableSubscriber not supported");
    }

    @Override
    public jakarta.jms.TopicSubscriber createDurableSubscriber(jakarta.jms.Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException("DurableSubscriber not supported");
    }

    @Override
    public jakarta.jms.MessageConsumer createDurableConsumer(jakarta.jms.Topic topic, String name) throws JMSException {
        throw new UnsupportedOperationException("DurableConsumer not supported");
    }

    @Override
    public jakarta.jms.MessageConsumer createDurableConsumer(jakarta.jms.Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException("DurableConsumer not supported");
    }

    @Override
    public jakarta.jms.MessageConsumer createSharedDurableConsumer(jakarta.jms.Topic topic, String name) throws JMSException {
        throw new UnsupportedOperationException("SharedDurableConsumer not supported");
    }

    @Override
    public jakarta.jms.MessageConsumer createSharedDurableConsumer(jakarta.jms.Topic topic, String name, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException("SharedDurableConsumer not supported");
    }

    @Override
    public jakarta.jms.MessageConsumer createSharedConsumer(jakarta.jms.Topic topic, String sharedSubscriptionName) throws JMSException {
        throw new UnsupportedOperationException("SharedConsumer not supported");
    }

    @Override
    public jakarta.jms.MessageConsumer createSharedConsumer(jakarta.jms.Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException("SharedConsumer not supported");
    }

    @Override
    public void recover() throws JMSException {
        try {
            javaxSession.recover();
        } catch (javax.jms.JMSException e) {
            throw new JMSException(e.getMessage(), e.getErrorCode());
        }
    }

    @Override
    public jakarta.jms.QueueBrowser createBrowser(jakarta.jms.Queue queue) throws JMSException {
        throw new UnsupportedOperationException("QueueBrowser not supported");
    }

    @Override
    public jakarta.jms.QueueBrowser createBrowser(jakarta.jms.Queue queue, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException("QueueBrowser not supported");
    }
}
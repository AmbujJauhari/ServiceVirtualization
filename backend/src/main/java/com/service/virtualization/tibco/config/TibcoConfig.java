package com.service.virtualization.tibco.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Connection;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import com.tibco.tibjms.TibjmsConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Configuration for TIBCO EMS integration.
 * 
 * This configuration creates a Jakarta-compatible wrapper around the 
 * TIBCO EMS javax.jms implementation to work with Spring Boot 3.x.
 */
@Configuration
@ConditionalOnProperty(name = "tibco.enabled", havingValue = "true", matchIfMissing = true)
public class TibcoConfig {

    @Value("${tibco.url:tcp://localhost:7222}")
    private String serverUrl;

    @Value("${tibco.username:admin}")
    private String username;

    @Value("${tibco.password:admin}")
    private String password;

    @Value("${tibco.connection.cache-size:10}")
    private int sessionCacheSize;

    /**
     * Jakarta wrapper for javax ExceptionListener
     */
    private static class JakartaExceptionListenerAdapter implements jakarta.jms.ExceptionListener {
        private final javax.jms.ExceptionListener javaxListener;

        public JakartaExceptionListenerAdapter(javax.jms.ExceptionListener javaxListener) {
            this.javaxListener = javaxListener;
        }

        @Override
        public void onException(jakarta.jms.JMSException exception) {
            // Convert Jakarta exception to javax exception
            javax.jms.JMSException javaxException = new javax.jms.JMSException(exception.getMessage(), exception.getErrorCode());
            if (exception.getCause() != null) {
                javaxException.initCause(exception.getCause());
            }
            javaxListener.onException(javaxException);
        }
    }

    /**
     * Javax wrapper for Jakarta ExceptionListener
     */
    private static class JavaxExceptionListenerAdapter implements javax.jms.ExceptionListener {
        private final jakarta.jms.ExceptionListener jakartaListener;

        public JavaxExceptionListenerAdapter(jakarta.jms.ExceptionListener jakartaListener) {
            this.jakartaListener = jakartaListener;
        }

        @Override
        public void onException(javax.jms.JMSException exception) {
            // Convert javax exception to Jakarta exception
            jakarta.jms.JMSException jakartaException = new jakarta.jms.JMSException(exception.getMessage(), exception.getErrorCode());
            if (exception.getCause() != null) {
                jakartaException.initCause(exception.getCause());
            }
            jakartaListener.onException(jakartaException);
        }
    }

    /**
     * Jakarta Queue adapter
     */
    private static class JavaxToJakartaQueueAdapter implements jakarta.jms.Queue {
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

    /**
     * Jakarta Topic adapter
     */
    private static class JavaxToJakartaTopicAdapter implements jakarta.jms.Topic {
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

    /**
     * Basic Jakarta Message adapter
     */
    private static class JavaxToJakartaMessageAdapter implements jakarta.jms.Message {
        private final javax.jms.Message javaxMessage;

        public JavaxToJakartaMessageAdapter(javax.jms.Message javaxMessage) {
            this.javaxMessage = javaxMessage;
        }

        @Override
        public String getJMSMessageID() throws JMSException {
            try {
                return javaxMessage.getJMSMessageID();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSMessageID(String id) throws JMSException {
            try {
                javaxMessage.setJMSMessageID(id);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public long getJMSTimestamp() throws JMSException {
            try {
                return javaxMessage.getJMSTimestamp();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSTimestamp(long timestamp) throws JMSException {
            try {
                javaxMessage.setJMSTimestamp(timestamp);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            try {
                return javaxMessage.getJMSCorrelationIDAsBytes();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
            try {
                javaxMessage.setJMSCorrelationIDAsBytes(correlationID);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSCorrelationID(String correlationID) throws JMSException {
            try {
                javaxMessage.setJMSCorrelationID(correlationID);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public String getJMSCorrelationID() throws JMSException {
            try {
                return javaxMessage.getJMSCorrelationID();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public jakarta.jms.Destination getJMSReplyTo() throws JMSException {
            try {
                javax.jms.Destination javaxDestination = javaxMessage.getJMSReplyTo();
                if (javaxDestination == null) {
                    return null;
                }
                return wrapJavaxDestination(javaxDestination);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSReplyTo(jakarta.jms.Destination replyTo) throws JMSException {
            try {
                javax.jms.Destination javaxDestination = extractJavaxDestination(replyTo);
                javaxMessage.setJMSReplyTo(javaxDestination);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public jakarta.jms.Destination getJMSDestination() throws JMSException {
            try {
                javax.jms.Destination javaxDestination = javaxMessage.getJMSDestination();
                if (javaxDestination == null) {
                    return null;
                }
                return wrapJavaxDestination(javaxDestination);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSDestination(jakarta.jms.Destination destination) throws JMSException {
            try {
                javax.jms.Destination javaxDestination = extractJavaxDestination(destination);
                javaxMessage.setJMSDestination(javaxDestination);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        private jakarta.jms.Destination wrapJavaxDestination(javax.jms.Destination javaxDestination) {
            if (javaxDestination instanceof javax.jms.Queue) {
                return new JavaxToJakartaQueueAdapter((javax.jms.Queue) javaxDestination);
            } else if (javaxDestination instanceof javax.jms.Topic) {
                return new JavaxToJakartaTopicAdapter((javax.jms.Topic) javaxDestination);
            } else {
                throw new IllegalArgumentException("Unsupported destination type: " + javaxDestination.getClass());
            }
        }

        private javax.jms.Destination extractJavaxDestination(jakarta.jms.Destination jakartaDestination) {
            if (jakartaDestination == null) {
                return null;
            } else if (jakartaDestination instanceof JavaxToJakartaQueueAdapter) {
                return ((JavaxToJakartaQueueAdapter) jakartaDestination).javaxQueue;
            } else if (jakartaDestination instanceof JavaxToJakartaTopicAdapter) {
                return ((JavaxToJakartaTopicAdapter) jakartaDestination).javaxTopic;
            } else {
                throw new IllegalArgumentException("Unsupported destination type: " + jakartaDestination.getClass());
            }
        }

        @Override
        public int getJMSDeliveryMode() throws JMSException {
            try {
                return javaxMessage.getJMSDeliveryMode();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
            try {
                javaxMessage.setJMSDeliveryMode(deliveryMode);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public boolean getJMSRedelivered() throws JMSException {
            try {
                return javaxMessage.getJMSRedelivered();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSRedelivered(boolean redelivered) throws JMSException {
            try {
                javaxMessage.setJMSRedelivered(redelivered);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public String getJMSType() throws JMSException {
            try {
                return javaxMessage.getJMSType();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSType(String type) throws JMSException {
            try {
                javaxMessage.setJMSType(type);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public long getJMSExpiration() throws JMSException {
            try {
                return javaxMessage.getJMSExpiration();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSExpiration(long expiration) throws JMSException {
            try {
                javaxMessage.setJMSExpiration(expiration);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public long getJMSDeliveryTime() throws JMSException {
            try {
                return javaxMessage.getJMSDeliveryTime();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
            try {
                javaxMessage.setJMSDeliveryTime(deliveryTime);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public int getJMSPriority() throws JMSException {
            try {
                return javaxMessage.getJMSPriority();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setJMSPriority(int priority) throws JMSException {
            try {
                javaxMessage.setJMSPriority(priority);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void clearProperties() throws JMSException {
            try {
                javaxMessage.clearProperties();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public boolean propertyExists(String name) throws JMSException {
            try {
                return javaxMessage.propertyExists(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public boolean getBooleanProperty(String name) throws JMSException {
            try {
                return javaxMessage.getBooleanProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public byte getByteProperty(String name) throws JMSException {
            try {
                return javaxMessage.getByteProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public short getShortProperty(String name) throws JMSException {
            try {
                return javaxMessage.getShortProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public int getIntProperty(String name) throws JMSException {
            try {
                return javaxMessage.getIntProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public long getLongProperty(String name) throws JMSException {
            try {
                return javaxMessage.getLongProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public float getFloatProperty(String name) throws JMSException {
            try {
                return javaxMessage.getFloatProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public double getDoubleProperty(String name) throws JMSException {
            try {
                return javaxMessage.getDoubleProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public String getStringProperty(String name) throws JMSException {
            try {
                return javaxMessage.getStringProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public Object getObjectProperty(String name) throws JMSException {
            try {
                return javaxMessage.getObjectProperty(name);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public java.util.Enumeration getPropertyNames() throws JMSException {
            try {
                return javaxMessage.getPropertyNames();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setBooleanProperty(String name, boolean value) throws JMSException {
            try {
                javaxMessage.setBooleanProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setByteProperty(String name, byte value) throws JMSException {
            try {
                javaxMessage.setByteProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setShortProperty(String name, short value) throws JMSException {
            try {
                javaxMessage.setShortProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setIntProperty(String name, int value) throws JMSException {
            try {
                javaxMessage.setIntProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setLongProperty(String name, long value) throws JMSException {
            try {
                javaxMessage.setLongProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setFloatProperty(String name, float value) throws JMSException {
            try {
                javaxMessage.setFloatProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setDoubleProperty(String name, double value) throws JMSException {
            try {
                javaxMessage.setDoubleProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setStringProperty(String name, String value) throws JMSException {
            try {
                javaxMessage.setStringProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setObjectProperty(String name, Object value) throws JMSException {
            try {
                javaxMessage.setObjectProperty(name, value);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void acknowledge() throws JMSException {
            try {
                javaxMessage.acknowledge();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void clearBody() throws JMSException {
            try {
                javaxMessage.clearBody();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public <T> T getBody(Class<T> c) throws JMSException {
            try {
                return javaxMessage.getBody(c);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public boolean isBodyAssignableTo(Class c) throws JMSException {
            try {
                return javaxMessage.isBodyAssignableTo(c);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }
    }

    /**
     * Jakarta wrapper for javax.jms.TextMessage to bridge the namespace gap
     */
    private static class JavaxToJakartaTextMessageAdapter extends JavaxToJakartaMessageAdapter implements jakarta.jms.TextMessage {
        private final javax.jms.TextMessage javaxTextMessage;

        public JavaxToJakartaTextMessageAdapter(javax.jms.TextMessage javaxTextMessage) {
            super(javaxTextMessage);
            this.javaxTextMessage = javaxTextMessage;
        }

        @Override
        public void setText(String string) throws JMSException {
            try {
                javaxTextMessage.setText(string);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public String getText() throws JMSException {
            try {
                return javaxTextMessage.getText();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }
    }

    /**
     * Jakarta MessageProducer adapter
     */
    private static class JavaxToJakartaMessageProducerAdapter implements jakarta.jms.MessageProducer {
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
                return ((JavaxToJakartaMessageAdapter) jakartaMessage).javaxMessage;
            } else if (jakartaMessage instanceof JavaxToJakartaTextMessageAdapter) {
                return ((JavaxToJakartaTextMessageAdapter) jakartaMessage).javaxTextMessage;
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

    /**
     * Jakarta MessageConsumer adapter
     */
    private static class JavaxToJakartaMessageConsumerAdapter implements jakarta.jms.MessageConsumer {
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

    /**
     * Basic Jakarta Session adapter for transaction support
     */
    private static class JavaxToJakartaSessionAdapter implements jakarta.jms.Session {
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

    /**
     * Jakarta wrapper for javax.jms.ConnectionFactory to bridge the namespace gap
     */
    private static class JavaxToJakartaConnectionFactoryAdapter implements ConnectionFactory {
        private final javax.jms.ConnectionFactory javaxConnectionFactory;

        public JavaxToJakartaConnectionFactoryAdapter(javax.jms.ConnectionFactory javaxConnectionFactory) {
            this.javaxConnectionFactory = javaxConnectionFactory;
        }

        @Override
        public Connection createConnection() throws JMSException {
            try {
                javax.jms.Connection javaxConnection = javaxConnectionFactory.createConnection();
                return new JavaxToJakartaConnectionAdapter(javaxConnection);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public Connection createConnection(String userName, String password) throws JMSException {
            try {
                javax.jms.Connection javaxConnection = javaxConnectionFactory.createConnection(userName, password);
                return new JavaxToJakartaConnectionAdapter(javaxConnection);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public JMSContext createContext() {
            throw new UnsupportedOperationException("JMSContext not supported");
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            throw new UnsupportedOperationException("JMSContext not supported");
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            throw new UnsupportedOperationException("JMSContext not supported");
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            throw new UnsupportedOperationException("JMSContext not supported");
        }
    }

    /**
     * Minimal Jakarta Connection wrapper - delegates basic operations to javax Connection
     */
    private static class JavaxToJakartaConnectionAdapter implements Connection {
        private final javax.jms.Connection javaxConnection;

        public JavaxToJakartaConnectionAdapter(javax.jms.Connection javaxConnection) {
            this.javaxConnection = javaxConnection;
        }

        @Override
        public jakarta.jms.Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
            try {
                javax.jms.Session javaxSession = javaxConnection.createSession(transacted, acknowledgeMode);
                return new JavaxToJakartaSessionAdapter(javaxSession);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public jakarta.jms.Session createSession(int sessionMode) throws JMSException {
            try {
                javax.jms.Session javaxSession = javaxConnection.createSession(sessionMode);
                return new JavaxToJakartaSessionAdapter(javaxSession);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public jakarta.jms.Session createSession() throws JMSException {
            try {
                javax.jms.Session javaxSession = javaxConnection.createSession();
                return new JavaxToJakartaSessionAdapter(javaxSession);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public String getClientID() throws JMSException {
            try {
                return javaxConnection.getClientID();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setClientID(String clientID) throws JMSException {
            try {
                javaxConnection.setClientID(clientID);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public jakarta.jms.ConnectionMetaData getMetaData() throws JMSException {
            throw new UnsupportedOperationException("ConnectionMetaData not supported");
        }

        @Override
        public jakarta.jms.ExceptionListener getExceptionListener() throws JMSException {
            try {
                javax.jms.ExceptionListener javaxListener = javaxConnection.getExceptionListener();
                if (javaxListener == null) {
                    return null;
                }
                // Create Jakarta wrapper for the javax ExceptionListener
                return new JakartaExceptionListenerAdapter(javaxListener);
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void setExceptionListener(jakarta.jms.ExceptionListener listener) throws JMSException {
            try {
                if (listener == null) {
                    javaxConnection.setExceptionListener(null);
                } else {
                    // Create javax wrapper for the Jakarta ExceptionListener
                    javax.jms.ExceptionListener javaxListener = new JavaxExceptionListenerAdapter(listener);
                    javaxConnection.setExceptionListener(javaxListener);
                }
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void start() throws JMSException {
            try {
                javaxConnection.start();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void stop() throws JMSException {
            try {
                javaxConnection.stop();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        @Override
        public void close() throws JMSException {
            try {
                javaxConnection.close();
            } catch (javax.jms.JMSException e) {
                throw new JMSException(e.getMessage(), e.getErrorCode());
            }
        }

        // Other methods throw UnsupportedOperationException
        @Override
        public jakarta.jms.ConnectionConsumer createConnectionConsumer(jakarta.jms.Destination destination, String messageSelector, jakarta.jms.ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw new UnsupportedOperationException("ConnectionConsumer not supported");
        }

        @Override
        public jakarta.jms.ConnectionConsumer createDurableConnectionConsumer(jakarta.jms.Topic topic, String subscriptionName, String messageSelector, jakarta.jms.ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw new UnsupportedOperationException("DurableConnectionConsumer not supported");
        }

        @Override
        public jakarta.jms.ConnectionConsumer createSharedConnectionConsumer(jakarta.jms.Topic topic, String subscriptionName, String messageSelector, jakarta.jms.ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw new UnsupportedOperationException("SharedConnectionConsumer not supported");
        }

        @Override
        public jakarta.jms.ConnectionConsumer createSharedDurableConnectionConsumer(jakarta.jms.Topic topic, String subscriptionName, String messageSelector, jakarta.jms.ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw new UnsupportedOperationException("SharedDurableConnectionConsumer not supported");
        }
    }

    /**
     * Creates a TIBCO EMS connection factory with Jakarta compatibility.
     */
    @Bean(name = "tibcoConnectionFactory")
    public ConnectionFactory tibcoConnectionFactory() throws javax.jms.JMSException {
        TibjmsConnectionFactory javaxConnectionFactory = new TibjmsConnectionFactory();
        javaxConnectionFactory.setServerUrl(serverUrl);
        javaxConnectionFactory.setUserName(username);
        javaxConnectionFactory.setUserPassword(password);
        
        // Set connection timeout properties
        javaxConnectionFactory.setConnAttemptTimeout(30000); // 30 seconds for connection attempts
        javaxConnectionFactory.setReconnAttemptTimeout(30000); // 30 seconds for reconnection attempts

        // Create Jakarta-compatible wrapper
        ConnectionFactory jakartaConnectionFactory = new JavaxToJakartaConnectionFactoryAdapter(javaxConnectionFactory);

        // Wrap with caching connection factory for better performance
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(jakartaConnectionFactory);
        cachingConnectionFactory.setSessionCacheSize(sessionCacheSize);
        cachingConnectionFactory.setReconnectOnException(true);
        cachingConnectionFactory.setCacheConsumers(false); // Disable consumer caching to prevent timeout issues
        cachingConnectionFactory.setCacheProducers(true); // Keep producer caching for performance

        return cachingConnectionFactory;
    }

    @Bean(name = "tibcoQueueJmsTemplate")
    public JmsTemplate queueJmsTemplate(@Qualifier("tibcoConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setPubSubDomain(false); // false for queues
        return template;
    }

    /**
     * JMS template for sending messages to TIBCO EMS topics
     */
    @Bean(name = "tibcoTopicJmsTemplate")
    public JmsTemplate topicJmsTemplate(@Qualifier("tibcoConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setPubSubDomain(true); // true for topics
        return template;
    }
} 

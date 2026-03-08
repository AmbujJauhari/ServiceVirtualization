package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

import javax.jms.Message;

public class JavaxToJakartaMessageAdapter implements jakarta.jms.Message {
    private final javax.jms.Message javaxMessage;

    public JavaxToJakartaMessageAdapter(javax.jms.Message javaxMessage) {
        this.javaxMessage = javaxMessage;
    }

    public Message getJavaxMessage() {
        return javaxMessage;
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
    public java.util.Enumeration<?> getPropertyNames() throws JMSException {
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

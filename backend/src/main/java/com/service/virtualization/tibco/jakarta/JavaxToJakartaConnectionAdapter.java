package com.service.virtualization.tibco.jakarta;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;

/**
 * Minimal Jakarta Connection wrapper - delegates basic operations to javax Connection
 */
public class JavaxToJakartaConnectionAdapter implements Connection {
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
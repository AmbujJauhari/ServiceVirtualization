package com.service.virtualization.tibco.jakarta;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;

public class JavaxToJakartaConnectionFactoryAdapter implements ConnectionFactory {
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



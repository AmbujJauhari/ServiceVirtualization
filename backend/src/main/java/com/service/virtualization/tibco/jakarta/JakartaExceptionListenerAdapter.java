package com.service.virtualization.tibco.jakarta;

public class JakartaExceptionListenerAdapter implements jakarta.jms.ExceptionListener {
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
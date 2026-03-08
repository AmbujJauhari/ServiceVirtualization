package com.service.virtualization.tibco.jakarta;

public class JavaxExceptionListenerAdapter implements javax.jms.ExceptionListener {
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

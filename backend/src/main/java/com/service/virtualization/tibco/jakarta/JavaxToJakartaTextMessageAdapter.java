package com.service.virtualization.tibco.jakarta;

import jakarta.jms.JMSException;

import javax.jms.TextMessage;

public class JavaxToJakartaTextMessageAdapter extends JavaxToJakartaMessageAdapter implements jakarta.jms.TextMessage {
    private final javax.jms.TextMessage javaxTextMessage;

    public JavaxToJakartaTextMessageAdapter(javax.jms.TextMessage javaxTextMessage) {
        super(javaxTextMessage);
        this.javaxTextMessage = javaxTextMessage;
    }

    public TextMessage getJavaxTextMessage() {
        return javaxTextMessage;
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


package com.service.virtualization.model;

/**
 * Enum representing the supported protocols for service virtualization
 */
public enum Protocol {
    HTTP,
    HTTPS,
    GRPC,
    SOAP,
    MQTT,
    WEBSOCKET,
    JMS,
    AMQP;
    
    public static Protocol getDefault() {
        return HTTP;
    }
}
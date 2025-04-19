package com.service.virtualization.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Health indicator for ActiveMQ
 */
@Component("activemq")
public class ActiveMQHealthIndicator implements HealthIndicator {

    @Value("${activemq.broker-url:localhost}")
    private String brokerUrl;
    
    @Value("${activemq.port:61616}")
    private int port;
    
    @Value("${activemq.connection-timeout:2000}")
    private int connectionTimeout;

    @Override
    public Health health() {
        try {
            // Extract host from broker URL if needed
            String host = brokerUrl;
            if (brokerUrl.contains("://")) {
                host = brokerUrl.split("://")[1].split(":")[0];
            }
            
            boolean isUp = checkConnection(host, port);
            
            if (isUp) {
                return Health.up()
                        .withDetail("broker", brokerUrl)
                        .withDetail("port", port)
                        .build();
            } else {
                return Health.down()
                        .withDetail("broker", brokerUrl)
                        .withDetail("port", port)
                        .withDetail("error", "Cannot connect to ActiveMQ")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("broker", brokerUrl)
                    .withDetail("port", port)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
    
    private boolean checkConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectionTimeout);
            return socket.isConnected();
        } catch (SocketTimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
} 
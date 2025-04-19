package com.service.virtualization.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

@Component
public class IBMMQHealthIndicator implements HealthIndicator {

    @Value("${ibmmq.host:localhost}")
    private String host;
    
    @Value("${ibmmq.port:1414}")
    private int port;
    
    @Value("${ibmmq.connection-timeout:2000}")
    private int connectionTimeout;

    @Override
    public Health health() {
        try {
            boolean isUp = checkConnection(host, port);
            
            if (isUp) {
                return Health.up()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .build();
            } else {
                return Health.down()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .withDetail("error", "Cannot connect to IBM MQ")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("host", host)
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
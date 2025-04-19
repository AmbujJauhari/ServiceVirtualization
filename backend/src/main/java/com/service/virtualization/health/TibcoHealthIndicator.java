package com.service.virtualization.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

@Component
public class TibcoHealthIndicator implements HealthIndicator {

    @Value("${tibco.server:localhost}")
    private String server;
    
    @Value("${tibco.port:7222}")
    private int port;
    
    @Value("${tibco.connection-timeout:2000}")
    private int connectionTimeout;

    @Override
    public Health health() {
        try {
            boolean isUp = checkConnection(server, port);
            
            if (isUp) {
                return Health.up()
                        .withDetail("server", server)
                        .withDetail("port", port)
                        .build();
            } else {
                return Health.down()
                        .withDetail("server", server)
                        .withDetail("port", port)
                        .withDetail("error", "Cannot connect to TIBCO EMS")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("server", server)
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
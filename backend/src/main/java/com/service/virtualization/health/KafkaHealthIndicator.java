package com.service.virtualization.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${kafka.connection-timeout:2000}")
    private int connectionTimeout;

    @Override
    public Health health() {
        try {
            // Parse the bootstrap servers string
            String[] servers = bootstrapServers.split(",");
            boolean anyServerUp = false;
            StringBuilder details = new StringBuilder();
            
            for (String server : servers) {
                String[] hostPort = server.trim().split(":");
                if (hostPort.length == 2) {
                    String host = hostPort[0];
                    int port = Integer.parseInt(hostPort[1]);
                    
                    boolean isUp = checkConnection(host, port);
                    if (isUp) {
                        anyServerUp = true;
                    }
                    
                    details.append(server).append(": ").append(isUp ? "UP" : "DOWN").append(", ");
                }
            }
            
            // Remove trailing comma and space
            if (details.length() > 0) {
                details.setLength(details.length() - 2);
            }
            
            if (anyServerUp) {
                return Health.up()
                        .withDetail("servers", details.toString())
                        .build();
            } else {
                return Health.down()
                        .withDetail("servers", details.toString())
                        .withDetail("error", "No Kafka servers available")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
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
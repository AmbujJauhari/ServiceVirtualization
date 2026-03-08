package com.service.virtualization.health;

import com.service.virtualization.activemq.config.ActiveMqServerConfig;
import com.service.virtualization.activemq.config.ActiveMqServerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health indicator for ActiveMQ.
 * Checks all configured servers from the registry.
 */
@Component("activemq")
@Profile("!activemq-disabled")
public class ActiveMQHealthIndicator implements HealthIndicator {

    private static final int CONNECTION_TIMEOUT_MS = 2000;

    @Autowired
    private ActiveMqServerRegistry serverRegistry;

    @Override
    public Health health() {
        Map<String, ActiveMqServerConfig> servers = serverRegistry.getAllServers();

        if (servers.isEmpty()) {
            return Health.unknown()
                    .withDetail("info", "No ActiveMQ servers configured (activemq.registry.*)")
                    .build();
        }

        Map<String, String> statuses = new LinkedHashMap<>();
        boolean anyUp = false;

        for (Map.Entry<String, ActiveMqServerConfig> entry : servers.entrySet()) {
            String name = entry.getKey();
            ActiveMqServerConfig cfg = entry.getValue();
            String[] hostPort = parseHostPort(cfg.getBrokerUrl());
            boolean up = hostPort != null && checkConnection(hostPort[0], Integer.parseInt(hostPort[1]));
            statuses.put(name, (up ? "UP" : "DOWN") + " (" + cfg.getBrokerUrl() + ")");
            if (up) anyUp = true;
        }

        Health.Builder builder = anyUp ? Health.up() : Health.down();
        statuses.forEach(builder::withDetail);
        return builder.build();
    }

    /** Parses host and port from a broker URL like tcp://host:61616 or ssl://host:61617 */
    private String[] parseHostPort(String url) {
        if (url == null) return null;
        try {
            String withoutScheme = url.replaceFirst("^[a-z]+://", "");
            // Strip any path or query components
            withoutScheme = withoutScheme.split("[/?]")[0];
            String[] parts = withoutScheme.split(":");
            if (parts.length == 2) return parts;
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean checkConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}

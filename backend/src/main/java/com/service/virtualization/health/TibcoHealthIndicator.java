package com.service.virtualization.health;

import com.service.virtualization.tibco.config.TibcoServerConfig;
import com.service.virtualization.tibco.config.TibcoServerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Profile("!tibco-disabled")
public class TibcoHealthIndicator implements HealthIndicator {

    private static final int CONNECTION_TIMEOUT_MS = 2000;

    @Autowired
    private TibcoServerRegistry serverRegistry;

    @Override
    public Health health() {
        Map<String, TibcoServerConfig> servers = serverRegistry.getAllServers();

        if (servers.isEmpty()) {
            return Health.unknown()
                    .withDetail("info", "No TIBCO EMS servers configured (tibco.registry.*)")
                    .build();
        }

        Map<String, String> statuses = new LinkedHashMap<>();
        boolean anyUp = false;

        for (Map.Entry<String, TibcoServerConfig> entry : servers.entrySet()) {
            String name = entry.getKey();
            TibcoServerConfig cfg = entry.getValue();
            String[] hostPort = parseHostPort(cfg.getUrl());
            boolean up = hostPort != null && checkConnection(hostPort[0], Integer.parseInt(hostPort[1]));
            statuses.put(name, (up ? "UP" : "DOWN") + " (" + cfg.getUrl() + ")");
            if (up) anyUp = true;
        }

        Health.Builder builder = anyUp ? Health.up() : Health.down();
        statuses.forEach(builder::withDetail);
        return builder.build();
    }

    /** Parses host and port from a Tibco URL like tcp://host:7222 or ssl://host:7243 */
    private String[] parseHostPort(String url) {
        if (url == null) return null;
        try {
            // Strip scheme (tcp:// or ssl://)
            String withoutScheme = url.replaceFirst("^[a-z]+://", "");
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

package com.service.virtualization.health;

import com.service.virtualization.ibmmq.config.IbmMqServerConfig;
import com.service.virtualization.ibmmq.config.IbmMqServerRegistry;
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
@Profile("!ibmmq-disabled")
public class IBMMQHealthIndicator implements HealthIndicator {

    private static final int CONNECTION_TIMEOUT_MS = 2000;

    @Autowired
    private IbmMqServerRegistry serverRegistry;

    @Override
    public Health health() {
        Map<String, IbmMqServerConfig> servers = serverRegistry.getAllServers();

        if (servers.isEmpty()) {
            return Health.unknown()
                    .withDetail("info", "No IBM MQ servers configured (ibmmq.registry.*)")
                    .build();
        }

        Map<String, String> statuses = new LinkedHashMap<>();
        boolean anyUp = false;

        for (Map.Entry<String, IbmMqServerConfig> entry : servers.entrySet()) {
            String name = entry.getKey();
            IbmMqServerConfig cfg = entry.getValue();
            boolean up = checkConnection(cfg.getHost(), cfg.getPort());
            statuses.put(name, (up ? "UP" : "DOWN") + " (" + cfg.getHost() + ":" + cfg.getPort() + ")");
            if (up) anyUp = true;
        }

        Health.Builder builder = anyUp ? Health.up() : Health.down();
        statuses.forEach(builder::withDetail);
        return builder.build();
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

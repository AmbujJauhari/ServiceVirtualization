package com.service.virtualization.health;

import com.service.virtualization.kafka.config.KafkaServerConfig;
import com.service.virtualization.kafka.config.KafkaServerRegistry;
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
@Profile("!kafka-disabled")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final int CONNECTION_TIMEOUT_MS = 2000;

    @Autowired
    private KafkaServerRegistry serverRegistry;

    @Override
    public Health health() {
        Map<String, KafkaServerConfig> clusters = serverRegistry.getAllClusters();

        if (clusters.isEmpty()) {
            return Health.unknown()
                    .withDetail("info", "No Kafka clusters configured (kafka.registry.*)")
                    .build();
        }

        Map<String, String> statuses = new LinkedHashMap<>();
        boolean anyUp = false;

        for (Map.Entry<String, KafkaServerConfig> entry : clusters.entrySet()) {
            String clusterName = entry.getKey();
            KafkaServerConfig cfg = entry.getValue();
            boolean clusterUp = false;

            // bootstrap-servers may be comma-separated: host1:port1,host2:port2
            for (String server : cfg.getBootstrapServers().split(",")) {
                String[] parts = server.trim().split(":");
                if (parts.length == 2) {
                    try {
                        boolean up = checkConnection(parts[0], Integer.parseInt(parts[1]));
                        if (up) clusterUp = true;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            statuses.put(clusterName, (clusterUp ? "UP" : "DOWN") + " (" + cfg.getBootstrapServers() + ")");
            if (clusterUp) anyUp = true;
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

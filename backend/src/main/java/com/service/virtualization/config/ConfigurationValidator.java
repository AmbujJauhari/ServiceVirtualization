package com.service.virtualization.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required operational properties are present at startup,
 * based on which protocols are actually enabled.
 *
 * Design rules:
 *  - Properties with product-wide defaults (ports, paths) are NOT validated here;
 *    they are always present via application.yml or team config.
 *  - Operational properties (URLs, credentials, server addresses) MUST be supplied
 *    by the team via their mounted /app/config/application.properties (ConfigMap).
 *  - Each protocol is independently optional; its properties are only required
 *    when its corresponding *-disabled profile is NOT active.
 *  - WireMock is only required when REST or SOAP is enabled (it is the engine for both).
 *  - Registry config (*.registry.*) is validated here for presence; individual entry
 *    properties are validated by each Registry bean at startup.
 */
@Component
public class ConfigurationValidator {

    @Autowired
    private Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        List<String> missing = new ArrayList<>();
        List<String> profiles = List.of(environment.getActiveProfiles());

        validateDatabase(missing, profiles);
        validateWireMock(missing, profiles);
        validateKafka(missing, profiles);
        validateIbmMq(missing, profiles);
        validateTibco(missing, profiles);
        validateActiveMq(missing, profiles);
        validateSchemaRegistry(missing, profiles);

        if (!missing.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Service Virtualization Platform startup failed — missing required configuration:\n");
            missing.forEach(p -> msg.append("  - ").append(p).append("\n"));
            msg.append("\nActive profiles: ").append(String.join(", ", profiles));
            msg.append("\nSupply these in your team's /app/config/application.properties (ConfigMap).");
            throw new IllegalStateException(msg.toString());
        }

        System.out.println("✅ Configuration validation passed. Active profiles: " + String.join(", ", profiles));
    }

    // -------------------------------------------------------------------------
    // Per-protocol validators
    // -------------------------------------------------------------------------

    private void validateDatabase(List<String> missing, List<String> profiles) {
        if (profiles.contains("mongodb") || (!profiles.contains("sybase") && profiles.isEmpty())) {
            require("spring.data.mongodb.uri", missing);
            require("spring.data.mongodb.database", missing);
        }
        if (profiles.contains("sybase")) {
            require("spring.datasource.url", missing);
            require("spring.datasource.username", missing);
            require("spring.datasource.password", missing);
        }
    }

    /**
     * WireMock is required when REST or SOAP is enabled.
     * Both REST and SOAP use the same embedded WireMock instance.
     */
    private void validateWireMock(List<String> missing, List<String> profiles) {
        boolean restEnabled = !profiles.contains("rest-disabled");
        boolean soapEnabled = !profiles.contains("soap-disabled");
        if (restEnabled || soapEnabled) {
            require("wiremock.server.host", missing);
            require("wiremock.server.port", missing);
        }
    }

    /**
     * Kafka requires at least one cluster in the registry when not disabled.
     * Individual cluster properties (bootstrap-servers etc.) are validated by KafkaServerRegistry.
     */
    private void validateKafka(List<String> missing, List<String> profiles) {
        if (!profiles.contains("kafka-disabled")) {
            if (!hasPropertyPrefix("kafka.registry.")) {
                missing.add("kafka.registry.<name>.bootstrap.servers (at least one cluster required)");
            }
        }
    }

    /**
     * IBM MQ requires at least one server in the registry when not disabled.
     * Individual server properties are validated by IbmMqServerRegistry.
     */
    private void validateIbmMq(List<String> missing, List<String> profiles) {
        if (!profiles.contains("ibmmq-disabled")) {
            if (!hasPropertyPrefix("ibmmq.registry.")) {
                missing.add("ibmmq.registry.<name>.host (at least one server required)");
            }
        }
    }

    /**
     * TIBCO requires at least one server in the registry when not disabled.
     * Individual server properties are validated by TibcoServerRegistry.
     */
    private void validateTibco(List<String> missing, List<String> profiles) {
        if (!profiles.contains("tibco-disabled")) {
            if (!hasPropertyPrefix("tibco.registry.")) {
                missing.add("tibco.registry.<name>.url (at least one server required)");
            }
        }
    }

    /**
     * ActiveMQ requires at least one server in the registry when not disabled.
     * Individual server properties are validated by ActiveMqServerRegistry.
     */
    private void validateActiveMq(List<String> missing, List<String> profiles) {
        if (!profiles.contains("activemq-disabled")) {
            if (!hasPropertyPrefix("activemq.registry.")) {
                missing.add("activemq.registry.<name>.broker-url (at least one server required)");
            }
        }
    }

    /**
     * Schema Registry is optional — only validate when SCHEMA_REGISTRY_URL is present.
     */
    private void validateSchemaRegistry(List<String> missing, List<String> profiles) {
        if (!profiles.contains("kafka-disabled") && present("schema.registry.url")) {
            require("schema.registry.auth.type", missing);

            String authType = environment.getProperty("schema.registry.auth.type", "");
            switch (authType.toLowerCase()) {
                case "basic":
                    require("schema.registry.auth.username", missing);
                    require("schema.registry.auth.password", missing);
                    break;
                case "apikey":
                case "bearer":
                    require("schema.registry.auth.api-key", missing);
                    break;
                case "none":
                case "":
                    break;
                default:
                    missing.add("SCHEMA_REGISTRY_AUTH_TYPE (invalid: '" + authType + "'; valid values: none, basic, apikey, bearer)");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void require(String key, List<String> missing) {
        if (!present(key)) {
            missing.add(key);
        }
    }

    private boolean present(String key) {
        String value = environment.getProperty(key);
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasPropertyPrefix(String prefix) {
        if (!(environment instanceof ConfigurableEnvironment)) {
            return false;
        }
        for (var source : ((ConfigurableEnvironment) environment).getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> eps) {
                for (String name : eps.getPropertyNames()) {
                    if (name.startsWith(prefix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

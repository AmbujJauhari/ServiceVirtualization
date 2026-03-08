package com.servicevirtualization.test.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads connection parameters from test.properties.
 * Each test class uses this to get bootstrap servers, topic names, and SSL settings
 * without hardcoding values.
 */
public class KafkaTestConfig {

    private static final String PROPERTIES_FILE = "test.properties";
    private static final Properties props = new Properties();

    static {
        try (InputStream in = KafkaTestConfig.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Cannot find " + PROPERTIES_FILE + " on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + PROPERTIES_FILE, e);
        }
    }

    // -------------------------------------------------------------------------
    // Margin — Events Instance
    // -------------------------------------------------------------------------

    public static String getMarginBootstrapServers() {
        return required("margin.events.kafka.bootstrapServers");
    }

    public static String getMarginTopic() {
        return required("margin.events.kafka.topic");
    }

    public static String getMarginConsumerGroup() {
        return required("margin.events.kafka.consumerGroup");
    }

    // -------------------------------------------------------------------------
    // Collateral — Events Instance
    // -------------------------------------------------------------------------

    public static String getCollateralBootstrapServers() {
        return required("collateral.events.kafka.bootstrapServers");
    }

    public static String getCollateralTopic() {
        return required("collateral.events.kafka.topic");
    }

    public static String getCollateralConsumerGroup() {
        return required("collateral.events.kafka.consumerGroup");
    }

    // -------------------------------------------------------------------------
    // TLS / SSL
    // -------------------------------------------------------------------------

    public static String getTrustStoreLocation() {
        return required("ssl.truststore.location");
    }

    public static String getTrustStorePassword() {
        return required("ssl.truststore.password");
    }

    public static String getEndpointIdentificationAlgorithm() {
        return props.getProperty("ssl.endpoint.identification.algorithm", "");
    }

    // -------------------------------------------------------------------------
    // Timeouts
    // -------------------------------------------------------------------------

    public static int getConnectionTimeout() {
        return Integer.parseInt(props.getProperty("test.connectionTimeout", "10000"));
    }

    public static long getPollTimeout() {
        return Long.parseLong(props.getProperty("test.pollTimeout", "10000"));
    }

    public static long getProducerTimeout() {
        return Long.parseLong(props.getProperty("test.producerTimeout", "10000"));
    }

    // -------------------------------------------------------------------------

    private static String required(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required property '" + key + "' is missing in " + PROPERTIES_FILE);
        }
        return value.trim();
    }
}

package com.servicevirtualization.test.tibco;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads connection parameters from test.properties.
 * Each test class uses this to get host, port, credentials, and TLS settings
 * without hardcoding values.
 */
public class TibcoTestConfig {

    private static final String PROPERTIES_FILE = "test.properties";
    private static final Properties props = new Properties();

    static {
        try (InputStream in = TibcoTestConfig.class
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
    // Margin — Server A
    // -------------------------------------------------------------------------

    public static String getMarginServerAHost() {
        return required("margin.servera.tibco.host");
    }

    public static int getMarginServerAPort() {
        return Integer.parseInt(required("margin.servera.tibco.port"));
    }

    public static String getMarginServerAUsername() {
        return required("margin.servera.tibco.username");
    }

    public static String getMarginServerAPassword() {
        return required("margin.servera.tibco.password");
    }

    public static String getMarginServerAQueue() {
        return required("margin.servera.tibco.queue");
    }

    public static String getMarginServerATopic() {
        return required("margin.servera.tibco.topic");
    }

    // -------------------------------------------------------------------------
    // Margin — Server B
    // -------------------------------------------------------------------------

    public static String getMarginServerBHost() {
        return required("margin.serverb.tibco.host");
    }

    public static int getMarginServerBPort() {
        return Integer.parseInt(required("margin.serverb.tibco.port"));
    }

    public static String getMarginServerBUsername() {
        return required("margin.serverb.tibco.username");
    }

    public static String getMarginServerBPassword() {
        return required("margin.serverb.tibco.password");
    }

    public static String getMarginServerBQueue() {
        return required("margin.serverb.tibco.queue");
    }

    public static String getMarginServerBTopic() {
        return required("margin.serverb.tibco.topic");
    }

    // -------------------------------------------------------------------------
    // TLS
    // -------------------------------------------------------------------------

    public static String getTrustStorePath() {
        return required("ssl.trustStorePath");
    }

    public static String getTrustStorePassword() {
        return required("ssl.trustStorePassword");
    }

    // -------------------------------------------------------------------------
    // Timeouts
    // -------------------------------------------------------------------------

    public static int getConnectionTimeout() {
        return Integer.parseInt(props.getProperty("test.connectionTimeout", "10000"));
    }

    public static int getMessageTimeout() {
        return Integer.parseInt(props.getProperty("test.messageTimeout", "5000"));
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

package com.servicevirtualization.test.ibmmq;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads connection parameters from test.properties.
 * Each test class uses this to get host, port, channel, and SSL settings
 * without hardcoding values.
 */
public class IBMMQTestConfig {

    private static final String PROPERTIES_FILE = "test.properties";
    private static final Properties props = new Properties();

    static {
        try (InputStream in = IBMMQTestConfig.class
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
    // Margin MQ
    // -------------------------------------------------------------------------

    public static String getMarginHost() {
        return required("margin.mq.host");
    }

    public static int getMarginPort() {
        return Integer.parseInt(required("margin.mq.port"));
    }

    public static String getMarginChannel() {
        return required("margin.mq.channel");
    }

    public static String getMarginQueueManager() {
        return required("margin.mq.queueManager");
    }

    public static String getMarginQueue() {
        return required("margin.mq.queue");
    }

    // -------------------------------------------------------------------------
    // Collateral MQ
    // -------------------------------------------------------------------------

    public static String getCollateralHost() {
        return required("collateral.mq.host");
    }

    public static int getCollateralPort() {
        return Integer.parseInt(required("collateral.mq.port"));
    }

    public static String getCollateralChannel() {
        return required("collateral.mq.channel");
    }

    public static String getCollateralQueueManager() {
        return required("collateral.mq.queueManager");
    }

    public static String getCollateralQueue() {
        return required("collateral.mq.queue");
    }

    // -------------------------------------------------------------------------
    // TLS
    // -------------------------------------------------------------------------

    public static String getSslCipherSuite() {
        return required("ssl.cipherSuite");
    }

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

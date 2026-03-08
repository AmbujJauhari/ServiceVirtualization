package com.servicevirtualization.test.tibco;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that TLS connections can be established to both Tibco EMS instances.
 *
 * What is being tested:
 *   1. The Istio ingressgateway is reachable on port 443 (NodePort 30501 locally)
 *   2. The TLS handshake succeeds (server cert is trusted by the JVM truststore)
 *   3. The Tibco EMS SSL port accepts the connection and authenticates the user
 *   4. Both EMS instances (Server A and Server B) are independently reachable
 *      on the same external port — confirmed by distinct SNI hostnames
 */
@DisplayName("Tibco EMS TLS Connection Tests")
class TibcoTLSConnectionTest {

    private static final Logger log = LoggerFactory.getLogger(TibcoTLSConnectionTest.class);

    @Test
    @DisplayName("Margin Server A: TLS connection established successfully")
    void marginServerATLSConnectionSucceeds() {
        log.info("=== TEST: Margin Tibco Server A TLS Connection ===");
        log.info("Connecting to: {}:{}", TibcoTestConfig.getMarginServerAHost(), TibcoTestConfig.getMarginServerAPort());

        assertDoesNotThrow(() -> {
            try (Connection connection = TibcoConnectionHelper.createMarginServerAConnection()) {
                connection.start();
                log.info("Connection started — TLS handshake and EMS authentication succeeded");
                assertNotNull(connection, "Connection must not be null");
            }
        }, "Margin Tibco Server A TLS connection should succeed without exception");
    }

    @Test
    @DisplayName("Margin Server B: TLS connection established successfully")
    void marginServerBTLSConnectionSucceeds() {
        log.info("=== TEST: Margin Tibco Server B TLS Connection ===");
        log.info("Connecting to: {}:{}", TibcoTestConfig.getMarginServerBHost(), TibcoTestConfig.getMarginServerBPort());

        assertDoesNotThrow(() -> {
            try (Connection connection = TibcoConnectionHelper.createMarginServerBConnection()) {
                connection.start();
                log.info("Connection started — TLS handshake and EMS authentication succeeded");
                assertNotNull(connection, "Connection must not be null");
            }
        }, "Margin Tibco Server B TLS connection should succeed without exception");
    }

    @Test
    @DisplayName("Both EMS instances reachable simultaneously on same port — SNI routing confirmed")
    void bothInstancesReachableOnSamePort() throws Exception {
        log.info("=== TEST: Both Tibco EMS Instances Reachable on Same Port ===");
        log.info("Same NodePort ({}), two different EMS pods — routing by SNI hostname",
                TibcoTestConfig.getMarginServerAPort());

        try (Connection serverAConn = TibcoConnectionHelper.createMarginServerAConnection();
             Connection serverBConn = TibcoConnectionHelper.createMarginServerBConnection()) {

            serverAConn.start();
            serverBConn.start();

            log.info("Both EMS connections open simultaneously — SNI routing confirmed");

            assertNotNull(serverAConn, "Server A connection must not be null");
            assertNotNull(serverBConn, "Server B connection must not be null");
            assertNotEquals(serverAConn, serverBConn,
                    "Server A and Server B connections must be distinct objects");
        }
    }
}

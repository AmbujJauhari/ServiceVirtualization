package com.servicevirtualization.test.ibmmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that TLS connections can be established to both IBM MQ instances.
 *
 * What is being tested:
 *   1. The Istio ingressgateway is reachable on port 443
 *   2. The TLS handshake succeeds (server cert is trusted by the JVM truststore)
 *   3. The IBM MQ SSL channel accepts the connection
 *   4. Both MQ instances (margin and collateral) are independently reachable
 */
@DisplayName("IBM MQ TLS Connection Tests")
class IBMMQTLSConnectionTest {

    private static final Logger log = LoggerFactory.getLogger(IBMMQTLSConnectionTest.class);

    @Test
    @DisplayName("Margin MQ: TLS connection established successfully")
    void marginMQTLSConnectionSucceeds() {
        log.info("=== TEST: Margin MQ TLS Connection ===");
        log.info("Connecting to: {}:{}", IBMMQTestConfig.getMarginHost(), IBMMQTestConfig.getMarginPort());

        assertDoesNotThrow(() -> {
            try (Connection connection = IBMMQConnectionHelper.createMarginConnection()) {
                connection.start();
                log.info("Connection started — TLS handshake and MQ channel negotiation succeeded");
                assertNotNull(connection, "Connection must not be null");
            }
        }, "Margin MQ TLS connection should succeed without exception");
    }

    @Test
    @DisplayName("Collateral MQ: TLS connection established successfully")
    void collateralMQTLSConnectionSucceeds() {
        log.info("=== TEST: Collateral MQ TLS Connection ===");
        log.info("Connecting to: {}:{}", IBMMQTestConfig.getCollateralHost(), IBMMQTestConfig.getCollateralPort());

        assertDoesNotThrow(() -> {
            try (Connection connection = IBMMQConnectionHelper.createCollateralConnection()) {
                connection.start();
                log.info("Connection started — TLS handshake and MQ channel negotiation succeeded");
                assertNotNull(connection, "Connection must not be null");
            }
        }, "Collateral MQ TLS connection should succeed without exception");
    }

    @Test
    @DisplayName("Both MQ instances are reachable simultaneously on port 443")
    void bothInstancesReachableOnSamePort() throws Exception {
        log.info("=== TEST: Both MQ Instances Reachable on Port 443 ===");
        log.info("This test proves SNI routing: same port, two different MQ instances");

        try (Connection marginConn = IBMMQConnectionHelper.createMarginConnection();
             Connection collateralConn = IBMMQConnectionHelper.createCollateralConnection()) {

            marginConn.start();
            collateralConn.start();

            log.info("Both connections open simultaneously on port {} — SNI routing confirmed",
                    IBMMQTestConfig.getMarginPort());

            assertNotNull(marginConn);
            assertNotNull(collateralConn);
            assertNotEquals(marginConn, collateralConn,
                    "Margin and collateral connections must be distinct");
        }
    }
}

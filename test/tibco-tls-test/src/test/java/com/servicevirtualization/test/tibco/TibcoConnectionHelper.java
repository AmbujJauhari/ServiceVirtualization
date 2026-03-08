package com.servicevirtualization.test.tibco;

import com.tibco.tibjms.TibjmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;

/**
 * Creates Tibco EMS JMS connections over TLS using Istio TLS PASSTHROUGH routing.
 *
 * Connection flow:
 *   Client → TLS(wildcard cert) → Istio gateway (reads SNI from ssl:// hostname, no decrypt)
 *          → Tibco EMS pod:7243 → pod terminates TLS end-to-end
 *
 * Istio reads the SNI hostname from the TLS ClientHello (it is the hostname
 * in the ssl:// URL) and routes to the correct EMS pod without decrypting traffic.
 *
 * The Tibco EMS pod presents the wildcard cert (*.service-virtualization.local)
 * mounted from the team's margin-tls-secret. The client truststore must contain
 * this same wildcard cert.
 *
 * TLS configuration is handled via the javax.net.ssl system properties:
 *   -Djavax.net.ssl.trustStore=<path>/truststore.jks
 *   -Djavax.net.ssl.trustStorePassword=changeit
 *
 * These are set in the Maven Surefire argLine (integration profile) or
 * in IntelliJ's run configuration VM options.
 */
public class TibcoConnectionHelper {

    private static final Logger log = LoggerFactory.getLogger(TibcoConnectionHelper.class);

    /**
     * Creates a TLS-enabled JMS connection to a given Tibco EMS instance.
     *
     * @param host     SNI hostname (e.g. servera.tibco.margin.service-virtualization.local)
     * @param port     NodePort mapped to the Istio gateway TLS port (e.g. 30501 for local K3s)
     * @param username EMS username
     * @param password EMS password
     * @return an open JMS Connection — caller must close it
     */
    public static Connection createTlsConnection(String host,
                                                  int port,
                                                  String username,
                                                  String password) throws JMSException {
        String url = "ssl://" + host + ":" + port;
        log.info("Connecting to Tibco EMS: url={} user={}", url, username);

        TibjmsConnectionFactory cf = new TibjmsConnectionFactory();
        cf.setServerUrl(url);

        // The SNI hostname is derived from the ssl:// URL by the Tibco EMS client.
        // Istio uses this SNI to route to the correct EMS pod (TLS PASSTHROUGH).
        // Explicitly setting the server name here as well for clarity, though it
        // is already embedded in the URL.
        cf.setSslServerName(host);

        // The Tibco EMS client honours javax.net.ssl.trustStore for its SSL context.
        // No additional per-factory truststore configuration is required because the
        // JVM-level truststore is set via the Surefire argLine / IntelliJ run config.
        // For fine-grained control, use cf.setSslTrustedCertificates("path/to/cert.pem")
        // instead of the JVM truststore.

        // Disable hostname verification for wildcard cert testing.
        // The wildcard cert CN is *.service-virtualization.local, which does not
        // directly match servera.tibco.margin.service-virtualization.local (too many levels).
        // In production, use a cert that covers the full hostname or enable verification.
        cf.setSslVerifyHostName(false);

        Connection connection = cf.createConnection(username, password);
        log.info("Connected successfully to Tibco EMS at {}", url);
        return connection;
    }

    /**
     * Convenience method: creates a TLS connection using Margin Server A config.
     */
    public static Connection createMarginServerAConnection() throws JMSException {
        return createTlsConnection(
                TibcoTestConfig.getMarginServerAHost(),
                TibcoTestConfig.getMarginServerAPort(),
                TibcoTestConfig.getMarginServerAUsername(),
                TibcoTestConfig.getMarginServerAPassword()
        );
    }

    /**
     * Convenience method: creates a TLS connection using Margin Server B config.
     */
    public static Connection createMarginServerBConnection() throws JMSException {
        return createTlsConnection(
                TibcoTestConfig.getMarginServerBHost(),
                TibcoTestConfig.getMarginServerBPort(),
                TibcoTestConfig.getMarginServerBUsername(),
                TibcoTestConfig.getMarginServerBPassword()
        );
    }
}

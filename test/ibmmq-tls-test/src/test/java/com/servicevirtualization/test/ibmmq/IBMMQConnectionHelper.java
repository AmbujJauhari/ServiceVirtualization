package com.servicevirtualization.test.ibmmq;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;

/**
 * Creates IBM MQ JMS connections over TLS using Istio TLS PASSTHROUGH routing.
 *
 * Connection flow:
 *   Client → TLS(ECDHE-RSA-AES256-GCM-SHA384) → Istio gateway (reads SNI, no decrypt)
 *          → IBM MQ pod:1414 → pod terminates TLS end-to-end
 *
 * Istio reads the SNI hostname from the TLS ClientHello and routes to the correct
 * MQ pod without decrypting traffic. The IBM MQ pod handles the full TLS handshake
 * using its auto-generated self-signed certificate.
 *
 * Prerequisites (run once after Helm install):
 *   1. Extract each pod's self-signed cert and import into truststore.jks
 *      (commands are in test.properties header)
 *   2. Set JVM properties (Maven Surefire sets these; add to IntelliJ run config):
 *      -Dcom.ibm.mq.cfg.preferTLS=true
 *      -Djavax.net.ssl.trustStore=<path>/truststore.jks
 *      -Djavax.net.ssl.trustStorePassword=changeit
 *
 * SSLPeerName is intentionally not set. When omitted the IBM MQ client skips
 * distinguished name (DN) checking entirely — the TLS handshake still validates
 * the certificate against the truststore but the CN/SAN is not checked against
 * a pattern. This is correct for dev/test where the wildcard cert CN does not
 * match the queue manager name. For production, set SSLPeerName to the expected
 * DN of the server certificate (e.g. "CN=icg.ibmmq.margin.company.com").
 */
public class IBMMQConnectionHelper {

    private static final Logger log = LoggerFactory.getLogger(IBMMQConnectionHelper.class);

    /**
     * Creates a TLS-enabled JMS connection to a given IBM MQ instance.
     *
     * @param host         SNI hostname (e.g. icg.ibmmq.margin.service-virtualization.local)
     * @param port         NodePort mapped to cluster port 443 (e.g. 32262 for local K3s)
     * @param channel      MQ server-connection channel (must have SSLCIPH configured)
     * @param queueManager Queue Manager name
     * @return an open JMS Connection — caller must close it
     */
    public static Connection createTlsConnection(String host,
                                                  int port,
                                                  String channel,
                                                  String queueManager) throws JMSException {
        log.info("Connecting to IBM MQ: host={} port={} channel={} qm={}",
                host, port, channel, queueManager);

        MQConnectionFactory cf = new MQConnectionFactory();
        cf.setHostName(host);
        cf.setPort(port);
        cf.setChannel(channel);
        cf.setQueueManager(queueManager);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);

        // Cipher suite for TLS PASSTHROUGH: must match SSLCIPH on the MQ channel.
        // TLS_RSA_* suites are not supported by Envoy's BoringSSL; use ECDHE.
        cf.setSSLCipherSuite(IBMMQTestConfig.getSslCipherSuite());

        // SSLPeerName intentionally not set — IBM MQ client skips DN checking
        // when this property is absent. The TLS handshake still validates the
        // certificate against the truststore (javax.net.ssl.trustStore).
        // For production, set: cf.setSSLPeerName("CN=<expected-hostname>")

        // The JVM truststore (javax.net.ssl.trustStore) must contain the IBM MQ pod's
        // self-signed cert. Set via -Djavax.net.ssl.trustStore in IntelliJ run config
        // or Maven Surefire argLine (see pom.xml integration profile).

        // No credentials needed: DEV.AUTHINFO has CHCKCLNT(NONE) and each channel has
        // an explicit CHLAUTH rule with CHCKCLNT(ASQMGR) that inherits this setting.
        // MCAUSER('app') on the channel provides the authorization identity for all
        // MQ object access without requiring the client to supply a username/password.
        Connection connection = cf.createConnection();
        log.info("Connected successfully to {} ({}:{})", queueManager, host, port);
        return connection;
    }

    /**
     * Convenience method: creates a TLS connection using Margin team config.
     */
    public static Connection createMarginConnection() throws JMSException {
        return createTlsConnection(
                IBMMQTestConfig.getMarginHost(),
                IBMMQTestConfig.getMarginPort(),
                IBMMQTestConfig.getMarginChannel(),
                IBMMQTestConfig.getMarginQueueManager()
        );
    }

    /**
     * Convenience method: creates a TLS connection using Collateral team config.
     */
    public static Connection createCollateralConnection() throws JMSException {
        return createTlsConnection(
                IBMMQTestConfig.getCollateralHost(),
                IBMMQTestConfig.getCollateralPort(),
                IBMMQTestConfig.getCollateralChannel(),
                IBMMQTestConfig.getCollateralQueueManager()
        );
    }
}

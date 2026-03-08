package com.service.virtualization.tibco.config;

import java.util.Objects;

/**
 * Configuration for a single TIBCO EMS server in the registry.
 * 
 * Supports both SSL and non-SSL connections:
 * - SSL connections require JKS keystore and truststore paths
 * - Non-SSL connections (tcp://) don't require certificates
 */
public class TibcoServerConfig {
    
    private final String name;
    private final String url;
    private final String username;
    private final String password;
    private final Integer timeout;
    private final SslConfig ssl;
    private final Boolean enabled;
    
    /**
     * SSL-specific configuration
     */
    public static class SslConfig {
        private final Boolean enabled;
        private final String jksPath;
        private final String jksPassword;
        private final String truststorePath;
        private final String truststorePassword;
        private final String protocol;
        private final Boolean verifyHostname;
        
        public SslConfig(Boolean enabled, String jksPath, String jksPassword,
                        String truststorePath, String truststorePassword,
                        String protocol, Boolean verifyHostname) {
            this.enabled = enabled;
            this.jksPath = jksPath;
            this.jksPassword = jksPassword;
            this.truststorePath = truststorePath;
            this.truststorePassword = truststorePassword;
            this.protocol = protocol != null ? protocol : "TLSv1.2";
            this.verifyHostname = verifyHostname != null ? verifyHostname : true;
        }
        
        public Boolean getEnabled() { return enabled; }
        public String getJksPath() { return jksPath; }
        public String getJksPassword() { return jksPassword; }
        public String getTruststorePath() { return truststorePath; }
        public String getTruststorePassword() { return truststorePassword; }
        public String getProtocol() { return protocol; }
        public Boolean getVerifyHostname() { return verifyHostname; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SslConfig sslConfig = (SslConfig) o;
            return Objects.equals(enabled, sslConfig.enabled) &&
                   Objects.equals(jksPath, sslConfig.jksPath) &&
                   Objects.equals(jksPassword, sslConfig.jksPassword) &&
                   Objects.equals(truststorePath, sslConfig.truststorePath) &&
                   Objects.equals(truststorePassword, sslConfig.truststorePassword) &&
                   Objects.equals(protocol, sslConfig.protocol) &&
                   Objects.equals(verifyHostname, sslConfig.verifyHostname);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(enabled, jksPath, jksPassword, truststorePath,
                              truststorePassword, protocol, verifyHostname);
        }
    }
    
    public TibcoServerConfig(String name, String url, String username, String password,
                            Integer timeout, SslConfig ssl, Boolean enabled) {
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.ssl = ssl;
        this.enabled = enabled != null ? enabled : true;
    }
    
    // Getters
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Integer getTimeout() { return timeout; }
    public SslConfig getSsl() { return ssl; }
    public Boolean getEnabled() { return enabled; }
    
    /**
     * Determines if SSL is enabled for this server.
     * 
     * Detection logic:
     * 1. If ssl.enabled is explicitly set, use that value
     * 2. Otherwise, auto-detect from URL scheme (ssl:// = true, tcp:// = false)
     * 
     * @return true if SSL should be used
     */
    public boolean isSslEnabled() {
        if (ssl != null && ssl.getEnabled() != null) {
            return ssl.getEnabled();
        }
        
        // Auto-detect from URL
        if (url != null) {
            return url.toLowerCase().startsWith("ssl://");
        }
        
        return false;
    }
    
    /**
     * Validates this server configuration.
     * 
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("TIBCO server name is required");
        }
        
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("TIBCO server URL is required for server: " + name);
        }
        
        if (!url.startsWith("ssl://") && !url.startsWith("tcp://")) {
            throw new IllegalStateException(
                "TIBCO server URL must start with 'ssl://' or 'tcp://' for server: " + name + 
                ", got: " + url
            );
        }
        
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException("TIBCO username is required for server: " + name);
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("TIBCO password is required for server: " + name);
        }
        
        // SSL-specific validation
        if (isSslEnabled()) {
            validateSslConfiguration();
        }
    }
    
    /**
     * Validates SSL-specific configuration.
     * Only called if SSL is enabled.
     *
     * JKS keystore (client certificate) is OPTIONAL — only required for mutual TLS
     * where the server mandates a client certificate (ssl_require_client_cert = true).
     * For one-way TLS, only a truststore is needed; if no truststore path is given,
     * the JVM's javax.net.ssl.trustStore system property is used instead.
     */
    private void validateSslConfiguration() {
        if (ssl == null) {
            throw new IllegalStateException(
                "SSL configuration block is required for server: " + name +
                " when the URL uses ssl:// scheme. " +
                "Set at least tibco.registry." + name + ".ssl.enabled=true"
            );
        }

        // Client keystore (mutual TLS) — validate only when a path is explicitly provided
        if (ssl.getJksPath() != null && !ssl.getJksPath().trim().isEmpty()) {
            if (ssl.getJksPassword() == null || ssl.getJksPassword().trim().isEmpty()) {
                throw new IllegalStateException(
                    "SSL JKS keystore password is required when jks.path is set for server: " + name +
                    " (tibco.registry." + name + ".ssl.jks.password)"
                );
            }
            java.io.File jksFile = new java.io.File(ssl.getJksPath());
            if (!jksFile.exists()) {
                throw new IllegalStateException(
                    "SSL JKS keystore file not found for server: " + name +
                    ", path: " + ssl.getJksPath() +
                    "\nEnsure the certificate secret/volume is properly mounted!"
                );
            }
            if (!jksFile.canRead()) {
                throw new IllegalStateException(
                    "SSL JKS keystore file is not readable for server: " + name +
                    ", path: " + ssl.getJksPath() + "\nCheck file permissions!"
                );
            }
        }

        // Truststore — validate only when a path is explicitly provided.
        // When omitted, TIBCO uses the JVM's javax.net.ssl.trustStore (set via JAVA_OPTS).
        if (ssl.getTruststorePath() != null && !ssl.getTruststorePath().trim().isEmpty()) {
            if (ssl.getTruststorePassword() == null || ssl.getTruststorePassword().trim().isEmpty()) {
                throw new IllegalStateException(
                    "SSL truststore password is required when truststore.path is set for server: " + name
                );
            }
            java.io.File truststoreFile = new java.io.File(ssl.getTruststorePath());
            if (!truststoreFile.exists()) {
                throw new IllegalStateException(
                    "SSL truststore file not found for server: " + name +
                    ", path: " + ssl.getTruststorePath()
                );
            }
            if (!truststoreFile.canRead()) {
                throw new IllegalStateException(
                    "SSL truststore file is not readable for server: " + name +
                    ", path: " + ssl.getTruststorePath()
                );
            }
        }
    }
    
    /**
     * Returns a string representation (without sensitive data)
     */
    @Override
    public String toString() {
        return "TibcoServerConfig{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" +
                ", timeout=" + timeout +
                ", ssl=" + (ssl != null ? "enabled=" + isSslEnabled() : "null") +
                ", enabled=" + enabled +
                '}';
    }
} 
package com.service.virtualization.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.service.virtualization.wiremock.WebhookResponseTransformer;
import com.service.virtualization.rest.service.RestWebhookService;

import jakarta.annotation.PreDestroy;

/**
 * Configuration for WireMock (REST protocol support)
 * Only loaded when rest-disabled profile is NOT active
 */
@Configuration
@Profile("!rest-disabled")
public class WireMockConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WireMockConfig.class);

    @Value("${wiremock.server.port:8081}")
    private int port;
    
    @Value("${wiremock.https-port:8443}")
    private int httpsPort;
    
    @Value("${wiremock.root-dir:./wiremock}")
    private String rootDir;
    
    @Value("${wiremock.auto-persist-stubs:false}")
    private boolean autoPersistStubs;
    
    @Value("${wiremock.keystore-filePath:#{null}}")
    private String keystorePath;
    
    @Value("${wiremock.keystore-password:#{null}}")
    private String keystorePassword;
    
    @Value("${wiremock.keystore-type:JKS}")
    private String keystoreType;
    
    @Autowired
    private RestWebhookService restWebhookService;
    
    private WireMockServer wireMockServer;
    
    /**
     * Creates and configures a WireMockServer with webhook support
     */
    @Bean(destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        logger.info("Starting embedded WireMock server on port {}", port);
        
        // Create directory if it doesn't exist
        File fileStoreDir = new File(rootDir);
        if (!fileStoreDir.exists()) {
            fileStoreDir.mkdirs();
        }
        
        // Create webhook transformer with injected service
        WebhookResponseTransformer webhookTransformer = new WebhookResponseTransformer(restWebhookService);
        
        // Configure WireMock with webhook transformer
        WireMockConfiguration config = WireMockConfiguration.options()
                .port(port)
                .withRootDirectory(rootDir)
                .extensions(webhookTransformer)  // Add webhook transformer
                .notifier(new ConsoleNotifier(true));  // Enable verbose logging
        
        // Configure HTTPS if keystore is available
        if (keystorePath != null && keystorePassword != null) {
            try {
                File keyStoreFile = new File(keystorePath);
                if (keyStoreFile.exists()) {
                    logger.info("Configuring HTTPS for WireMock on port {}", httpsPort);
                    
                    // Load keystore for validation purposes only
                    KeyStore keyStore = KeyStore.getInstance(keystoreType);
                    try (FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
                        keyStore.load(keyStoreInputStream, keystorePassword.toCharArray());
                    }
                    
                    // Use native WireMock HTTPS configuration
                    config.httpsPort(httpsPort)
                          .keystoreType(keystoreType)
                          .keystorePath(keystorePath)
                          .keystorePassword(keystorePassword);
                    
                    logger.info("HTTPS configured successfully");
                } else {
                    logger.warn("Keystore file not found at: {}", keystorePath);
                }
            } catch (Exception e) {
                logger.error("Failed to configure HTTPS for WireMock", e);
            }
        }
        
        WireMockServer server = new WireMockServer(config);
        this.wireMockServer = server;
        
        try {
            server.start();
            logger.info("Embedded WireMock server started successfully on HTTP port {} and HTTPS port {}", 
                       port, httpsPort);
            
            // Set the static webhook service for the transformer
            WebhookResponseTransformer.setStaticWebhookService(restWebhookService);
            
        } catch (Exception e) {
            logger.error("Failed to start WireMock server", e);
            throw new RuntimeException("Failed to start WireMock server", e);
        }
        
        return server;
    }
    
    @PreDestroy
    public void stopWireMockServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            logger.info("Stopping WireMock server...");
            wireMockServer.stop();
            logger.info("WireMock server stopped");
        }
    }

    /**
     * Configure HTTPS for WireMock using certificate data from database
     */
    public void configureHttps(byte[] certificateData, String certificatePassword) {
        try {
            // Create temporary keystore from certificate data
            Path tempKeystore = Files.createTempFile("wiremock-keystore-", ".p12");
            Files.write(tempKeystore, certificateData);
            
            // Configure HTTPS keystore parameters
            System.setProperty("javax.net.ssl.keyStore", tempKeystore.toString());
            System.setProperty("javax.net.ssl.keyStorePassword", certificatePassword);
            System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
            
            // Clean up temp file when VM exits
            tempKeystore.toFile().deleteOnExit();
            
            logger.info("HTTPS configured successfully with certificate from database");
        } catch (IOException e) {
            logger.error("Failed to configure HTTPS with certificate data", e);
        }
    }
} 
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import jakarta.annotation.PreDestroy;

/**
 * Configuration for WireMock server
 */
@Configuration
public class WireMockConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WireMockConfig.class);

    @Value("${wiremock.port:8090}")
    private int port;
    
    @Value("${wiremock.https-port:8443}")
    private int httpsPort;
    
    @Value("${wiremock.root-dir:./wiremock}")
    private String rootDir;
    
    @Value("${wiremock.auto-persist-stubs:false}")
    private boolean autoPersistStubs;
    
    @Value("${wiremock.keystore-path:#{null}}")
    private String keystorePath;
    
    @Value("${wiremock.keystore-password:#{null}}")
    private String keystorePassword;
    
    @Value("${wiremock.keystore-type:JKS}")
    private String keystoreType;
    
    private WireMockServer wireMockServer;
    
    /**
     * Creates and configures a WireMockServer
     */
    @Bean(destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        // Create directory if it doesn't exist
        File fileStoreDir = new File(rootDir);
        if (!fileStoreDir.exists()) {
            fileStoreDir.mkdirs();
        }
        
        // Configure WireMock
        WireMockConfiguration config = WireMockConfiguration.options()
                .port(port)
                .withRootDirectory(rootDir);
                // .disableRequestJournal() // Uncomment to disable request journal if needed
        
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
        
        // Configure stub persistence based on settings
        if (!autoPersistStubs) {
            // Add a console notifier for better logging
            config.notifier(new ConsoleNotifier(true));
            
            // We'll handle recordings manually via our API
            // WireMock doesn't have a direct option to disable auto-creation
            // of stubs from recordings, but we control the recording process
        }
        
        WireMockServer server = new WireMockServer(config);
        server.start();
        logger.info("WireMock server started on HTTP port {} and HTTPS port {}", port, httpsPort);
        
        return server;
    }
    
    @PreDestroy
    public void stopWireMockServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
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
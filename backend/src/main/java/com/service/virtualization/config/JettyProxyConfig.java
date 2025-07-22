package com.service.virtualization.config;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jetty Proxy Servlet configuration for declarative proxy functionality
 * Provides a mature, production-ready proxy solution that forwards requests to WireMock
 */
@Configuration
public class JettyProxyConfig {

    private static final Logger logger = LoggerFactory.getLogger(JettyProxyConfig.class);

    @Value("${wiremock.server.host:localhost}")
    private String wiremockHost;

    @Value("${wiremock.server.port:8081}")
    private int wiremockPort;

    @Value("${proxy.connect-timeout:5000}")
    private String connectTimeout;

    @Value("${proxy.idle-timeout:30000}")
    private String idleTimeout;

    @Value("${proxy.timeout:30000}")
    private String timeout;

    /**
     * Configure Jetty ProxyServlet for /proxy/** endpoints
     * This servlet will handle all proxy functionality declaratively
     */
    @Bean
    public ServletRegistrationBean<ProxyServlet> proxyServletRegistration() {
        logger.info("Configuring Jetty Proxy Servlet for WireMock at {}:{}", wiremockHost, wiremockPort);

        // Create the proxy servlet
        ProxyServlet proxyServlet = new ProxyServlet();
        
        // Register servlet for /proxy/* path pattern
        ServletRegistrationBean<ProxyServlet> registration = 
            new ServletRegistrationBean<>(proxyServlet, "/proxy/*");

        // Declarative proxy configuration
        String proxyTo = String.format("http://%s:%d", wiremockHost, wiremockPort);
        registration.addInitParameter("proxyTo", proxyTo);
        
        // Remove /proxy prefix before forwarding to target
        registration.addInitParameter("prefix", "/proxy");
        
        // Timeout configurations
        registration.addInitParameter("connectTimeout", connectTimeout);
        registration.addInitParameter("timeout", timeout);
        registration.addInitParameter("idleTimeout", idleTimeout);
        
        // Proxy behavior settings
        registration.addInitParameter("preserveHost", "false");  // Don't preserve original Host header
        registration.addInitParameter("hostHeader", wiremockHost + ":" + wiremockPort);  // Set target host
        registration.addInitParameter("viaHost", "service-virtualization-proxy");  // Via header
        
        // Logging settings
        registration.addInitParameter("log", "true");  // Enable request logging
        
        // Load on startup
        registration.setLoadOnStartup(1);
        
        // Set name for identification
        registration.setName("jetty-proxy-servlet");

        logger.info("Jetty Proxy Servlet configured:");
        logger.info("  - Proxy Path: /proxy/*");
        logger.info("  - Target URL: {}", proxyTo);
        logger.info("  - Connect Timeout: {}ms", connectTimeout);
        logger.info("  - Request Timeout: {}ms", timeout);
        logger.info("  - Idle Timeout: {}ms", idleTimeout);

        return registration;
    }
} 
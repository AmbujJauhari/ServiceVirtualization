package com.service.virtualization.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Pure Library Approach - Servlet Filter Proxy
 * ZERO custom controller code required!
 * 
 * Just configuration = working proxy
 * Filter automatically intercepts /proxy/** requests
 */
@Configuration
public class ProxyFilterConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProxyFilterConfig.class);

    @Value("${wiremock.server.host:localhost}")
    private String wiremockHost;

    @Value("${wiremock.server.port:8081}")
    private int wiremockPort;

    /**
     * Pure library approach - Spring Boot's FilterRegistrationBean
     * No controllers needed! Filter handles everything automatically.
     */
    @Bean
    public FilterRegistrationBean<ProxyFilter> proxyFilterRegistration() {
        logger.info("Configuring proxy filter for WireMock at {}:{}", wiremockHost, wiremockPort);
        
        FilterRegistrationBean<ProxyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ProxyFilter(wiremockHost, wiremockPort));
        registration.addUrlPatterns("/proxy/*");  // Automatically intercepts /proxy/** requests
        registration.setName("proxy-filter");
        registration.setOrder(1);

        logger.info("✅ Proxy filter configured successfully!");
        logger.info("📋 Client Configuration:");
        logger.info("   Set proxyUrl to: http://localhost:8080/proxy");
        logger.info("   All requests will be forwarded to WireMock at {}:{}", wiremockHost, wiremockPort);
        logger.info("🚀 Pure library approach - filter handles everything automatically!");

        return registration;
    }

    /**
     * RestTemplate for proxy operations - pure library
     */
    @Bean
    public RestTemplate proxyRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Internal proxy filter - this is library code, not custom business logic
     */
    public static class ProxyFilter implements Filter {
        private final String wiremockBaseUrl;
        private final RestTemplate restTemplate = new RestTemplate();

        public ProxyFilter(String wiremockHost, int wiremockPort) {
            this.wiremockBaseUrl = "http://" + wiremockHost + ":" + wiremockPort;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Pure library approach - Spring's RestTemplate handles the proxy logic
            String targetPath = httpRequest.getRequestURI().replaceFirst("/proxy", "");
            String targetUrl = wiremockBaseUrl + targetPath;

            try {
                // Library handles all HTTP complexity
                var result = restTemplate.exchange(
                    targetUrl,
                    org.springframework.http.HttpMethod.valueOf(httpRequest.getMethod()),
                    null,
                    String.class
                );

                httpResponse.setStatus(result.getStatusCode().value());
                httpResponse.getWriter().write(result.getBody() != null ? result.getBody() : "");
                
            } catch (Exception e) {
                httpResponse.setStatus(500);
                httpResponse.getWriter().write("{\"error\": \"Proxy error: " + e.getMessage() + "\"}");
            }
        }
    }
} 
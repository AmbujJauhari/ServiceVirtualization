package com.service.virtualization.rest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for interacting with WireMock admin APIs
 */
@Service
public class WireMockAdminService {

    private static final Logger logger = LoggerFactory.getLogger(WireMockAdminService.class);

    private final RestTemplate restTemplate;
    private final String wiremockBaseUrl;

    public WireMockAdminService(@Value("${wiremock.server.host}") String wiremockHost,
                               @Value("${wiremock.server.port}") int wiremockPort,
                               RestTemplateBuilder restTemplateBuilder) {
        this.wiremockBaseUrl = String.format("http://%s:%d", wiremockHost, wiremockPort);
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Get all registered stub IDs from WireMock
     * 
     * @return Set of registered stub IDs, empty set if WireMock is unavailable
     */
    public Set<String> getRegisteredStubIds() {
        try {
            logger.debug("Fetching registered stub IDs from WireMock");
            
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    wiremockBaseUrl + "/__admin/mappings", 
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> mappings = (List<Map<String, Object>>) responseBody.get("mappings");
                
                if (mappings != null) {
                    Set<String> stubIds = mappings.stream()
                            .map(mapping -> (String) mapping.get("id"))
                            .filter(id -> id != null)
                            .collect(Collectors.toSet());
                    
                    logger.debug("Retrieved {} registered stub IDs from WireMock", stubIds.size());
                    return stubIds;
                }
            }
            
            logger.warn("Unexpected response from WireMock mappings endpoint: {}", response.getStatusCode());
            return Collections.emptySet();
            
        } catch (RestClientException e) {
            logger.warn("Failed to connect to WireMock server at {}: {}", wiremockBaseUrl, e.getMessage());
            return Collections.emptySet();
        } catch (Exception e) {
            logger.error("Unexpected error while fetching WireMock mappings: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Check if a specific stub is registered with WireMock
     * 
     * @param stubId the stub ID to check
     * @return true if registered, false if not registered or WireMock is unavailable
     */
    public boolean isStubRegistered(String stubId) {
        try {
            logger.debug("Checking if stub {} is registered with WireMock", stubId);
            
            ResponseEntity<String> response = restTemplate.getForEntity(
                    wiremockBaseUrl + "/__admin/mappings/" + stubId,
                    String.class
            );
            
            boolean isRegistered = response.getStatusCode() == HttpStatus.OK;
            logger.debug("Stub {} registration status: {}", stubId, isRegistered);
            return isRegistered;
            
        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                logger.debug("Stub {} not found in WireMock", stubId);
                return false;
            }
            logger.warn("Failed to check stub {} registration with WireMock: {}", stubId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while checking stub {} registration: {}", stubId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if WireMock server is available
     * 
     * @return true if WireMock is responding, false otherwise
     */
    public boolean isWireMockAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    wiremockBaseUrl + "/__admin/health",
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("WireMock health check failed: {}", e.getMessage());
            return false;
        }
    }
} 
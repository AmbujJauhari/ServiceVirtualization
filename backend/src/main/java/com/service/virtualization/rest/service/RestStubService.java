package com.service.virtualization.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.rest.model.RestStub;
import com.service.virtualization.rest.repository.RestStubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of StubService that manages stubs using WireMock and database storage
 */
@Service
public class RestStubService {

    private static final Logger logger = LoggerFactory.getLogger(RestStubService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestStubRepository restStubRepository;
    private final RestTemplate restTemplate;
    private final String wiremockBaseUrl;

    public RestStubService(RestStubRepository restStubRepository,
                           @Value("${wiremock.server.host}") String wiremockHost,
                           @Value("${wiremock.server.port}") int wiremockPort) {
        this.restStubRepository = restStubRepository;
        this.restTemplate = new RestTemplate();
        this.wiremockBaseUrl = String.format("http://%s:%d", wiremockHost, wiremockPort);
    }

    public RestStub createStub(RestStub stub) {
        logger.info("Creating stub: {}", stub.name());

        // Set default values if not provided
        String id = stub.id();
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        // Create a new stub with current timestamps
        stub = new RestStub(
                id,
                stub.name(),
                stub.description(),
                stub.userId(),
                stub.behindProxy(),
                stub.protocol(),
                stub.tags(),
                stub.status(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                id, // Use the same ID for WireMock mapping
                stub.matchConditions(),
                stub.response()
        );

        // Register stub with remote WireMock
        registerWithWireMock(stub);

        // Save to repository
        return restStubRepository.save(stub);
    }

    public RestStub updateStub(RestStub stub) {
        logger.info("Updating stub: {}", stub.name());

        // Verify stub exists
        Optional<RestStub> existingStub = findStubById(stub.id());
        if (existingStub.isEmpty()) {
            throw new IllegalArgumentException("Stub not found with ID: " + stub.id());
        }

        // Create updated stub with current timestamp
        stub = new RestStub(
                stub.id(),
                stub.name(),
                stub.description(),
                stub.userId(),
                stub.behindProxy(),
                stub.protocol(),
                stub.tags(),
                stub.status(),
                stub.createdAt(),
                LocalDateTime.now(),
                stub.id(), // Use the same ID for WireMock mapping
                stub.matchConditions(),
                stub.response()
        );

        // Remove existing WireMock mapping and create a new one
        deleteWireMockMapping(stub.id());
        registerWithWireMock(stub);

        // Update in repository
        return restStubRepository.save(stub);
    }

    public Optional<RestStub> findStubById(String id) {
        return restStubRepository.findById(id);
    }

    public List<RestStub> findAllStubs() {
        return restStubRepository.findAll();
    }

    public List<RestStub> findStubsByUserId(String userId) {
        return restStubRepository.findByUserId(userId);
    }

    public void deleteStub(String id) {
        logger.info("Deleting stub with ID: {}", id);

        // Find stub to get WireMock ID
        Optional<RestStub> stub = findStubById(id);
        if (stub.isPresent()) {
            // Remove from WireMock
            deleteWireMockMapping(id);

            // Delete from repository
            restStubRepository.deleteById(id);
        } else {
            logger.warn("Stub not found with ID: {}", id);
        }
    }

    private void deleteWireMockMapping(String id) {
        try {
            restTemplate.delete(wiremockBaseUrl + "/__admin/mappings/" + id);
        } catch (Exception e) {
            logger.warn("Failed to delete WireMock mapping for stub: {}", id, e);
        }
    }

    /**
     * Register stub with remote WireMock service
     */
    private void registerWithWireMock(RestStub stub) {
        try {
            // Extract match conditions
            Map<String, Object> matchConditions = stub.matchConditions();
            String method = (String) matchConditions.getOrDefault("method", "GET");
            String url = (String) matchConditions.getOrDefault("url", "/");
            String urlMatchType = (String) matchConditions.getOrDefault("urlMatchType", "exact");
            int priority = (int) matchConditions.getOrDefault("priority", 1);

            // Build request matcher
            Map<String, Object> request = new HashMap<>();

            // Add URL matching
            switch (urlMatchType) {
                case "exact":
                    request.put("url", url);
                    break;
            case "regex":
                    request.put("urlPattern", url);
                break;
                case "glob":
                    request.put("urlPath", url);
                break;
        }

            // Add method matching
            request.put("method", method);

            // Add headers matching if present
            List<Map<String, Object>> headers = (List<Map<String, Object>>) matchConditions.getOrDefault("headers", Collections.emptyList());
            if (!headers.isEmpty()) {
                Map<String, Object> headersPattern = new HashMap<>();
            for (Map<String, Object> header : headers) {
                String name = (String) header.get("name");
                String value = (String) header.get("value");
                    String matchType = (String) header.getOrDefault("matchType", "exact");

                    Map<String, Object> headerPattern = new HashMap<>();
                    switch (matchType) {
                        case "exact":
                            headerPattern.put("equalTo", value);
                            break;
                        case "regex":
                            headerPattern.put("matches", value);
                            break;
                        case "contains":
                            headerPattern.put("contains", value);
                            break;
                    }
                    headersPattern.put(name, headerPattern);
                }
                request.put("headers", headersPattern);
            }

            // Add query parameters matching if present
            List<Map<String, Object>> queryParams = (List<Map<String, Object>>) matchConditions.getOrDefault("queryParams", Collections.emptyList());
            if (!queryParams.isEmpty()) {
                Map<String, Object> queryPattern = new HashMap<>();
                for (Map<String, Object> param : queryParams) {
                    String name = (String) param.get("name");
                    String value = (String) param.get("value");
                    String matchType = (String) param.getOrDefault("matchType", "exact");

                    Map<String, Object> paramPattern = new HashMap<>();
                    switch (matchType) {
                        case "exact":
                            paramPattern.put("equalTo", value);
                            break;
                        case "regex":
                            paramPattern.put("matches", value);
                            break;
                        case "contains":
                            paramPattern.put("contains", value);
                            break;
                    }
                    queryPattern.put(name, paramPattern);
                }
                request.put("queryParameters", queryPattern);
            }

            // Add body matching if present
            String body = (String) matchConditions.get("body");
            String bodyMatchType = (String) matchConditions.getOrDefault("bodyMatchType", "exact");
            if (body != null && !body.isEmpty()) {
                Map<String, Object> bodyPattern = new HashMap<>();
                switch (bodyMatchType) {
                    case "exact":
                        bodyPattern.put("equalTo", body);
                        break;
                    case "json":
                        bodyPattern.put("matchesJsonSchema", body);
                        break;
                    case "jsonpath":
                        bodyPattern.put("matchesJsonPath", body);
                        break;
                    case "xpath":
                        bodyPattern.put("matchesXPath", body);
                        break;
                    case "contains":
                        bodyPattern.put("contains", body);
                        break;
                    case "regex":
                        bodyPattern.put("matches", body);
                        break;
                }
                request.put("bodyPatterns", Collections.singletonList(bodyPattern));
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> stubResponse = stub.response();

            // Add status code
            response.put("status", stubResponse.getOrDefault("status", 200));

            // Add response headers if present
            List<Map<String, Object>> responseHeadersList = (List<Map<String, Object>>) stubResponse.getOrDefault("headers", Collections.emptyList());
            if (!responseHeadersList.isEmpty()) {
                Map<String, String> responseHeadersMap = new HashMap<>();
                for (Map<String, Object> header : responseHeadersList) {
                    responseHeadersMap.put((String) header.get("name"), (String) header.get("value"));
                }
                response.put("headers", responseHeadersMap);
            }

            // Add response body if present
            String responseBody = (String) stubResponse.get("body");
            if (responseBody != null && !responseBody.isEmpty()) {
                response.put("body", responseBody);
            }

            // Add content type if specified
            String contentType = (String) stubResponse.get("contentType");
            if (contentType != null && !contentType.isEmpty()) {
                response.put("headers", new HashMap<>(Map.of("Content-Type", contentType)));
            }

            // Build the complete stub mapping
            Map<String, Object> stubMapping = new HashMap<>();
            stubMapping.put("id", stub.id());
            stubMapping.put("priority", priority);
            stubMapping.put("request", request);
            stubMapping.put("response", response);

            // Log the final request payload
            try {
                String requestPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stubMapping);
                logger.info("Registering stub with WireMock. Request payload:\n{}", requestPayload);
            } catch (Exception e) {
                logger.warn("Failed to log request payload", e);
            }

            // Register with WireMock
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(stubMapping, httpHeaders);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    wiremockBaseUrl + "/__admin/mappings",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to register stub with WireMock: " + responseEntity.getBody());
            }

        } catch (Exception e) {
            logger.error("Failed to register stub with WireMock", e);
            throw new RuntimeException("Failed to register stub with WireMock", e);
        }
    }
} 
package com.service.virtualization.rest.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.service.virtualization.rest.model.RestStub;
import com.service.virtualization.rest.repository.RestStubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of StubService that manages stubs using WireMock and database storage
 */
@Service
public class RestStubService {

    private static final Logger logger = LoggerFactory.getLogger(RestStubService.class);

    private final RestStubRepository restStubRepository;
    private final WireMockServer wireMockServer;

    public RestStubService(RestStubRepository restStubRepository,
                           WireMockServer wireMockServer) {
        this.restStubRepository = restStubRepository;
        this.wireMockServer = wireMockServer;
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
                stub.wiremockMappingId(),
                stub.matchConditions(),
                stub.response()
        );

        // Register stub with WireMock
        StubMapping stubMapping = registerWithWireMock(stub);

        // Create updated stub with WireMock mapping ID
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
                stubMapping.getId().toString(),
                stub.matchConditions(),
                stub.response()
        );

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
                stub.wiremockMappingId(),
                stub.matchConditions(),
                stub.response()
        );

        // Remove existing WireMock mapping and create a new one
        String wiremockId = existingStub.get().wiremockMappingId();
        if (wiremockId != null) {
            wireMockServer.removeStubMapping(UUID.fromString(wiremockId));
        }

        StubMapping stubMapping = registerWithWireMock(stub);

        // Create updated stub with new WireMock mapping ID
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
                stubMapping.getId().toString(),
                stub.matchConditions(),
                stub.response()
        );

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
            String wiremockId = stub.get().wiremockMappingId();
            if (wiremockId != null) {
                wireMockServer.removeStubMapping(UUID.fromString(wiremockId));
            }

            // Delete from repository
            restStubRepository.deleteById(id);
        } else {
            logger.warn("Stub not found with ID: {}", id);
        }
    }

    /**
     * Register stub with WireMock server
     */
    private StubMapping registerWithWireMock(RestStub stub) {
        // Create request matching
        MappingBuilder mappingBuilder;

        // Extract method and service path from request data
        String method = getMethod(stub);
        String servicePath = getServicePath(stub);

        // Determine URL matching strategy based on matchConditions.urlMatchType
        String urlMatchType = getUrlMatchType(stub);

        mappingBuilder = switch (method.toUpperCase()) {
            case "GET" -> WireMock.get(createUrlMatcher(servicePath, urlMatchType));
            case "POST" -> WireMock.post(createUrlMatcher(servicePath, urlMatchType));
            case "PUT" -> WireMock.put(createUrlMatcher(servicePath, urlMatchType));
            case "DELETE" -> WireMock.delete(createUrlMatcher(servicePath, urlMatchType));
            case "PATCH" -> WireMock.patch(createUrlMatcher(servicePath, urlMatchType));
            default -> WireMock.any(createUrlMatcher(servicePath, urlMatchType));
        };

        // Add request headers
        if (stub.matchConditions().containsKey("headers")) {
            addRequestHeaders(mappingBuilder, stub);
        }

        // Add request body if present
        String requestBody = getRequestBody(stub);
        String bodyMatchType = getBodyMatchType(stub);
        if (requestBody != null && !requestBody.isEmpty()) {
            addRequestBodyMatching(mappingBuilder, requestBody, bodyMatchType);
        }

        // Create response
        ResponseDefinitionBuilder responseBuilder = WireMock.aResponse()
                .withStatus(getResponseStatus(stub));

        // Add response headers
        if (stub.response().containsKey("headers")) {
            addResponseHeaders(responseBuilder, stub);
        }

        // Check if this is a callback response
        checkForCallback(stub);

        // Register with WireMock
        mappingBuilder.willReturn(responseBuilder);
        StubMapping stubMapping = mappingBuilder.build();
        wireMockServer.addStubMapping(stubMapping);
        return stubMapping;
    }

    // Create appropriate URL matcher based on matchType
    private UrlPattern createUrlMatcher(String servicePath, String matchType) {
        switch (matchType.toLowerCase()) {
            case "regex":
                return WireMock.urlMatching(servicePath);
            case "glob":
                // Convert glob pattern to regex
                String regex = servicePath.replace("*", "[^/]*").replace("**", ".*");
                return WireMock.urlMatching(regex);
            case "exact":
            default:
                return WireMock.urlEqualTo(servicePath);
        }
    }

    // Add request body matching based on bodyMatchType
    private void addRequestBodyMatching(MappingBuilder mappingBuilder, String requestBody, String bodyMatchType) {
        switch (bodyMatchType.toLowerCase()) {
            case "regex":
                mappingBuilder.withRequestBody(WireMock.matching(requestBody));
                break;
            case "jsonpath":
                mappingBuilder.withRequestBody(WireMock.matchingJsonPath(requestBody));
                break;
            case "xpath":
                mappingBuilder.withRequestBody(WireMock.matchingXPath(requestBody));
                break;
            case "contains":
                mappingBuilder.withRequestBody(WireMock.containing(requestBody));
                break;
            case "exact":
            default:
                mappingBuilder.withRequestBody(WireMock.equalTo(requestBody));
                break;
        }
    }

    // Add request headers from stub to mapping builder
    @SuppressWarnings("unchecked")
    private void addRequestHeaders(MappingBuilder mappingBuilder, RestStub stub) {
        List<Map<String, Object>> headers = (List<Map<String, Object>>) stub.matchConditions().get("headers");
        if (headers != null) {
            for (Map<String, Object> header : headers) {
                String name = (String) header.get("name");
                String value = (String) header.get("value");
                String matchType = header.containsKey("matchType") ? (String) header.get("matchType") : "exact";

                if (name != null && value != null) {
                    switch (matchType.toLowerCase()) {
                        case "regex":
                            mappingBuilder.withHeader(name, WireMock.matching(value));
                            break;
                        case "contains":
                            mappingBuilder.withHeader(name, WireMock.containing(value));
                            break;
                        case "exact":
                        default:
                            mappingBuilder.withHeader(name, WireMock.equalTo(value));
                            break;
                    }
                }
            }
        }
    }

    // Add response headers from stub to response builder
    @SuppressWarnings("unchecked")
    private void addResponseHeaders(ResponseDefinitionBuilder responseBuilder, RestStub stub) {
        List<Map<String, Object>> headers = (List<Map<String, Object>>) stub.response().get("headers");
        if (headers != null) {
            for (Map<String, Object> header : headers) {
                String name = (String) header.get("name");
                String value = (String) header.get("value");

                if (name != null && value != null) {
                    responseBuilder.withHeader(name, value);
                }
            }
        }
    }

    // Helper methods to extract data from the stub record using new format

    private String getMethod(RestStub stub) {
        if (stub.matchConditions().containsKey("method")) {
            return (String) stub.matchConditions().get("method");
        }
        return "GET";
    }

    private String getServicePath(RestStub stub) {
        if (stub.matchConditions().containsKey("url")) {
            return (String) stub.matchConditions().get("url");
        }
        return "/";
    }

    private String getUrlMatchType(RestStub stub) {
        if (stub.matchConditions().containsKey("urlMatchType")) {
            return (String) stub.matchConditions().get("urlMatchType");
        }
        return "exact";
    }

    private String getBodyMatchType(RestStub stub) {
        if (stub.matchConditions().containsKey("bodyMatchType")) {
            return (String) stub.matchConditions().get("bodyMatchType");
        }
        return "exact";
    }

    private String getRequestBody(RestStub stub) {
        if (stub.matchConditions().containsKey("body")) {
            return (String) stub.matchConditions().get("body");
        }
        return "";
    }

    private String getResponseBody(RestStub stub) {
        if (stub.response().containsKey("body")) {
            return (String) stub.response().get("body");
        }
        return "";
    }

    private String getResponseContentType(RestStub stub) {
        if (stub.response().containsKey("contentType")) {
            return (String) stub.response().get("contentType");
        }
        return "application/json";
    }

    private int getResponseStatus(RestStub stub) {
        if (stub.response().containsKey("status")) {
            Object status = stub.response().get("status");
            if (status instanceof Integer) {
                return (Integer) status;
            } else if (status instanceof String) {
                return Integer.parseInt((String) status);
            }
        }
        return 200;
    }

    private void checkForCallback(RestStub stub) {
        Map<String, Object> response = stub.response();
        if (response != null && response.containsKey("callback")) {
            Map<String, Object> callback = (Map<String, Object>) response.get("callback");
            String callbackUrl = (String) callback.get("url");
            String callbackMethod = (String) callback.getOrDefault("method", "POST");

            // This will store the original callback data
            response.put("_original_callback", Map.of(
                "url", callbackUrl,
                "method", callbackMethod
            ));

            // Configure a simple response that indicates a callback will be made
            response.put("body", "Callback will be forwarded to " + callbackUrl);
            response.put("headers", Map.of(
                "Content-Type", "text/plain",
                "X-Callback-Url", callbackUrl,
                "X-Callback-Method", callbackMethod,
                "X-Callback-Type", "forward-request"
            ));

            logger.info("Configured callback forwarding to {} with method {}", callbackUrl, callbackMethod);
        }
    }
} 
package com.service.virtualization.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.dto.DtoConverter;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.rest.dto.RestStubDTO;
import com.service.virtualization.rest.model.RestStub;
import com.service.virtualization.rest.repository.RestStubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of StubService that manages stubs using WireMock and database storage
 * Only active when rest-disabled profile is NOT active
 */
@Service
@Profile("!rest-disabled")
public class RestStubService {

    private static final Logger logger = LoggerFactory.getLogger(RestStubService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestStubRepository restStubRepository;
    private final WireMockAdminService wireMockAdminService;

    public RestStubService(RestStubRepository restStubRepository,
                           WireMockAdminService wireMockAdminService) {
        this.restStubRepository = restStubRepository;
        this.wireMockAdminService = wireMockAdminService;
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
                stub.response(),
                stub.webhookUrl()  // Include webhook URL
        );

        // Register stub with remote WireMock only if it's active
        if (stub.status() == StubStatus.ACTIVE) {
            logger.info("Registering new active stub {} with WireMock", stub.id());
            registerWithWireMock(stub);
        } else {
            logger.info("Creating inactive stub {}, skipping WireMock registration", stub.id());
        }

        // Save to repository
        return restStubRepository.save(stub);
    }

    public RestStub updateStub(RestStub stub) {
        logger.info("Updating stub: {}", stub.name());

        // Verify stub exists
        Optional<RestStub> existingStubOpt = findStubById(stub.id());
        if (existingStubOpt.isEmpty()) {
            throw new IllegalArgumentException("Stub not found with ID: " + stub.id());
        }

        RestStub existingStub = existingStubOpt.get();

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
                stub.response(),
                stub.webhookUrl()  // Include webhook URL
        );

        // Handle WireMock registration based on status changes
        handleWireMockRegistration(existingStub, stub);

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

    /**
     * Find all stubs and check their registration status with WireMock
     *
     * @return List of RestStubDTO with registration status checked
     */
    public List<RestStubDTO> findAllStubsWithRegistrationStatus() {
        logger.debug("Finding all stubs with WireMock registration status");

        // Get all stubs from repository
        List<RestStub> stubs = restStubRepository.findAll();

        if (stubs.isEmpty()) {
            logger.debug("No stubs found in repository");
            return Collections.emptyList();
        }

        // Get registered stub IDs from WireMock
        Set<String> registeredStubIds = wireMockAdminService.getRegisteredStubIds();

        // Convert stubs to DTOs and update status based on registration
        List<RestStubDTO> stubDTOs = stubs.stream()
                .map(stub -> {
                    RestStubDTO dto = DtoConverter.fromRestStub(stub);

                    // Check if stub is registered in WireMock
                    if (!registeredStubIds.contains(stub.id())) {
                        // Create new DTO with updated status
                        dto = new RestStubDTO(
                                dto.id(),
                                dto.name(),
                                dto.description(),
                                dto.userId(),
                                dto.behindProxy(),
                                dto.protocol(),
                                dto.tags(),
                                StubStatus.STUB_NOT_REGISTERED.name(), // Update status
                                dto.createdAt(),
                                dto.updatedAt(),
                                dto.wiremockMappingId(),
                                dto.matchConditions(),
                                dto.response(),
                                dto.webhookUrl()  // Include webhook URL
                        );
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        long notRegisteredCount = stubDTOs.stream()
                .mapToLong(dto -> StubStatus.STUB_NOT_REGISTERED.name().equals(dto.status()) ? 1 : 0)
                .sum();

        logger.info("Found {} stubs, {} not registered with WireMock",
                stubDTOs.size(), notRegisteredCount);

        return stubDTOs;
    }

    /**
     * Find stubs by user ID and check their registration status with WireMock
     *
     * @param userId the user ID to filter by
     * @return List of RestStubDTO with registration status checked
     */
    public List<RestStubDTO> findStubsByUserIdWithRegistrationStatus(String userId) {
        logger.debug("Finding stubs for user {} with WireMock registration status", userId);

        // Get stubs by user ID from repository
        List<RestStub> stubs = restStubRepository.findByUserId(userId);

        if (stubs.isEmpty()) {
            logger.debug("No stubs found for user {}", userId);
            return Collections.emptyList();
        }

        // Get registered stub IDs from WireMock
        Set<String> registeredStubIds = wireMockAdminService.getRegisteredStubIds();

        // Convert stubs to DTOs and update status based on registration
        List<RestStubDTO> stubDTOs = stubs.stream()
                .map(stub -> {
                    RestStubDTO dto = DtoConverter.fromRestStub(stub);

                    // Check if stub is registered in WireMock
                    if (!registeredStubIds.contains(stub.id())) {
                        // Create new DTO with updated status
                        dto = new RestStubDTO(
                                dto.id(),
                                dto.name(),
                                dto.description(),
                                dto.userId(),
                                dto.behindProxy(),
                                dto.protocol(),
                                dto.tags(),
                                StubStatus.STUB_NOT_REGISTERED.name(), // Update status
                                dto.createdAt(),
                                dto.updatedAt(),
                                dto.wiremockMappingId(),
                                dto.matchConditions(),
                                dto.response(),
                                dto.webhookUrl()  // Include webhook URL
                        );
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        long notRegisteredCount = stubDTOs.stream()
                .mapToLong(dto -> StubStatus.STUB_NOT_REGISTERED.name().equals(dto.status()) ? 1 : 0)
                .sum();

        logger.info("Found {} stubs for user {}, {} not registered with WireMock",
                stubDTOs.size(), userId, notRegisteredCount);

        return stubDTOs;
    }

    public void deleteStub(String id) {
        logger.info("Deleting stub with ID: {}", id);

        // Find stub to get WireMock ID
        Optional<RestStub> stub = findStubById(id);
        if (stub.isPresent()) {
            // Remove from WireMock
            wireMockAdminService.deleteWireMockMapping(id);

            // Delete from repository
            restStubRepository.deleteById(id);
        } else {
            logger.warn("Stub not found with ID: {}", id);
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
                case "urlPath":
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

            // Add webhook transformer if webhook URL is configured
            if (stub.hasWebhook()) {
                logger.info("Adding webhook transformer for stub {} with URL: {}", stub.id(), stub.webhookUrl());

                // Create transformer parameters
                Map<String, Object> transformerParams = new HashMap<>();
                transformerParams.put("webhookUrl", stub.webhookUrl());
                transformerParams.put("stubId", stub.id());

                // Add the transformer to the response with correct WireMock format
                response.put("transformers", Collections.singletonList("webhook-response-transformer"));
                response.put("transformerParameters", transformerParams);
            }

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

            ResponseEntity<String> responseEntity = wireMockAdminService.postWiremockMappings(entity);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to register stub with WireMock: " + responseEntity.getBody());
            }

        } catch (Exception e) {
            logger.error("Failed to register stub with WireMock", e);
            throw new RuntimeException("Failed to register stub with WireMock", e);
        }
    }


    /**
     * Handle WireMock registration/deregistration based on status changes
     */
    private void handleWireMockRegistration(RestStub existingStub, RestStub updatedStub) {
        boolean wasActive = existingStub.status() == StubStatus.ACTIVE;
        boolean isActive = updatedStub.status() == StubStatus.ACTIVE;

        logger.debug("Handling WireMock registration for stub {}: was {} -> now {}",
                updatedStub.id(), existingStub.status(), updatedStub.status());

        if (wasActive && !isActive) {
            // Stub was active, now inactive - deregister from WireMock
            logger.info("Deregistering stub {} from WireMock (status changed from {} to {})",
                    updatedStub.id(), existingStub.status(), updatedStub.status());
            wireMockAdminService.deleteWireMockMapping(updatedStub.id());

        } else if (!wasActive && isActive) {
            // Stub was inactive, now active - register with WireMock
            logger.info("Registering stub {} with WireMock (status changed from {} to {})",
                    updatedStub.id(), existingStub.status(), updatedStub.status());
            registerWithWireMock(updatedStub);

        } else if (isActive) {
            // Stub remains active - update registration (delete and re-register)
            logger.info("Updating WireMock registration for active stub {}", updatedStub.id());
            wireMockAdminService.deleteWireMockMapping(updatedStub.id());
            registerWithWireMock(updatedStub);

        } else {
            // Stub remains inactive - no WireMock action needed
            logger.debug("Stub {} remains inactive, no WireMock action needed", updatedStub.id());
        }
    }
} 
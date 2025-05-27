package com.service.virtualization.soap.service.impl;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.rest.service.WireMockAdminService;
import com.service.virtualization.soap.SoapStub;
import com.service.virtualization.soap.SoapStubRepository;
import com.service.virtualization.soap.service.SoapStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SOAP stub service implementation using WireMock infrastructure
 */
@Service
public class SoapStubServiceImpl implements SoapStubService {

    private static final Logger logger = LoggerFactory.getLogger(SoapStubServiceImpl.class);

    @Autowired
    private final SoapStubRepository soapStubRepository;

    @Autowired
    private final WireMockAdminService wireMockAdminService;

    public SoapStubServiceImpl(SoapStubRepository soapStubRepository, WireMockAdminService wireMockAdminService) {
        this.soapStubRepository = soapStubRepository;
        this.wireMockAdminService = wireMockAdminService;
    }


    @Override
    public SoapStub createStub(SoapStub stub) {
        logger.info("Creating SOAP stub: {}", stub.name());

        // Create stub with current timestamp
        stub = new SoapStub(
                stub.id(),
                stub.name(),
                stub.description(),
                stub.userId(),
                stub.behindProxy(),
                stub.protocol(),
                stub.tags(),
                stub.status(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                stub.url(),
                stub.soapAction(),
                stub.webhookUrl(),
                stub.matchConditions(),
                stub.response()
        );

        // Save to database
        stub = soapStubRepository.save(stub);

        // Register stub with WireMock only if it's active
        if (stub.status() == StubStatus.ACTIVE) {
            logger.info("Registering new active SOAP stub {} with WireMock", stub.id());
            registerWithWireMock(stub);
        } else {
            logger.info("Creating inactive SOAP stub {}, skipping WireMock registration", stub.id());
        }

        return stub;
    }

    @Override
    public SoapStub updateStub(SoapStub stub) {
        logger.info("Updating SOAP stub: {}", stub.name());

        // Verify stub exists
        Optional<SoapStub> existingStubOpt = findStubById(stub.id());
        if (existingStubOpt.isEmpty()) {
            throw new IllegalArgumentException("SOAP stub not found with ID: " + stub.id());
        }

        SoapStub existingStub = existingStubOpt.get();

        // Create updated stub with current timestamp
        stub = new SoapStub(
                stub.id(),
                stub.name(),
                stub.description(),
                stub.userId(),
                stub.behindProxy(),
                stub.protocol(),
                stub.tags(),
                stub.status(),
                existingStub.createdAt(),
                LocalDateTime.now(),
                existingStub.wiremockMappingId(),
                stub.url(),
                stub.soapAction(),
                stub.webhookUrl(),
                stub.matchConditions(),
                stub.response()
        );

        // Handle WireMock registration based on status
        if (existingStub.status() == StubStatus.ACTIVE && stub.status() == StubStatus.INACTIVE) {
            // Deactivating: remove from WireMock
            logger.info("Deactivating SOAP stub {}, removing from WireMock", stub.id());
            deregisterFromWireMock(stub);
        } else if (existingStub.status() == StubStatus.INACTIVE && stub.status() == StubStatus.ACTIVE) {
            // Activating: register with WireMock
            logger.info("Activating SOAP stub {}, registering with WireMock", stub.id());
            registerWithWireMock(stub);
        } else if (stub.status() == StubStatus.ACTIVE) {
            // Active stub being updated: re-register
            logger.info("Updating active SOAP stub {}, re-registering with WireMock", stub.id());
            deregisterFromWireMock(stub);
            registerWithWireMock(stub);
        }
        // If inactive stub being updated, no WireMock action needed

        // Save to database
        return soapStubRepository.save(stub);
    }

    @Override
    public void deleteStub(String id) {
        logger.info("Deleting SOAP stub: {}", id);

        Optional<SoapStub> stubOpt = findStubById(id);
        if (stubOpt.isPresent()) {
            SoapStub stub = stubOpt.get();

            // Remove from WireMock if active
            if (stub.status() == StubStatus.ACTIVE) {
                deregisterFromWireMock(stub);
            }

            // Remove from database
            soapStubRepository.deleteById(id);
        }
    }

    @Override
    public Optional<SoapStub> findStubById(String id) {
        return soapStubRepository.findById(id);
    }

    @Override
    public List<SoapStub> findAllStubs() {
        return soapStubRepository.findAll();
    }

    @Override
    public List<SoapStub> findStubsByUserId(String userId) {
        return soapStubRepository.findByUserId(userId);
    }

    @Override
    public List<SoapStub> findStubsByStatus(StubStatus status) {
        return soapStubRepository.findByStatus(status);
    }

    @Override
    public List<SoapStub> findStubsByUrl(String urlPattern) {
        return soapStubRepository.findByUrlContaining(urlPattern);
    }

    @Override
    public SoapStub updateStubStatus(String id, StubStatus status) {
        logger.info("Updating SOAP stub {} status to {}", id, status);

        Optional<SoapStub> stubOpt = findStubById(id);
        if (stubOpt.isEmpty()) {
            throw new IllegalArgumentException("SOAP stub not found with ID: " + id);
        }

        SoapStub existingStub = stubOpt.get();
        SoapStub updatedStub = existingStub.withStatus(status);

        return updateStub(updatedStub);
    }

    /**
     * Register SOAP stub with WireMock
     */
    private void registerWithWireMock(SoapStub stub) {
        try {
            logger.info("Registering SOAP stub {} with WireMock", stub.id());

            Map<String, Object> mapping = new HashMap<>();
            mapping.put("id", stub.id());
            mapping.put("priority", 1);

            // SOAP request matching (always POST)
            Map<String, Object> request = new HashMap<>();
            request.put("method", "POST");
            request.put("url", stub.url());

            // Add SOAPAction header matching if provided
            if (stub.soapAction() != null && !stub.soapAction().trim().isEmpty()) {
                Map<String, Object> headers = new HashMap<>();
                headers.put("SOAPAction", Map.of("equalTo", stub.soapAction()));
                request.put("headers", headers);
            }

            // Add XML body matching if provided
            if (stub.matchConditions().containsKey("body")) {
                String bodyContent = (String) stub.matchConditions().get("body");
                String bodyMatchType = (String) stub.matchConditions().getOrDefault("bodyMatchType", "Equals");
                if (bodyContent != null && !bodyContent.trim().isEmpty()) {
                    Map<String, Object> bodyPattern = new HashMap<>();
                    switch (bodyMatchType.toLowerCase()) {
                        case "equals":
                            bodyPattern.put("equalTo", bodyContent);
                            break;
                        case "xpath":
                            bodyPattern.put("matchesXPath", bodyContent);
                            break;
                        case "contains":
                            bodyPattern.put("contains", bodyContent);
                            break;
                        case "regex":
                            bodyPattern.put("matches", bodyContent);
                            break;
                    }
                    request.put("bodyPatterns", Collections.singletonList(bodyPattern));
                }
            }

            mapping.put("request", request);

            // SOAP response
            Map<String, Object> response = new HashMap<>();
            response.put("status", stub.response().getOrDefault("status", 200));

            // Set XML content type
            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Content-Type", "text/xml; charset=utf-8");
            response.put("headers", responseHeaders);

            // Add response body
            if (stub.response().containsKey("body")) {
                response.put("body", stub.response().get("body"));
            }

            // Add webhook transformer if webhook URL is configured
            if (stub.hasWebhook()) {
                logger.info("Adding webhook transformer for SOAP stub {} with URL: {}", stub.id(), stub.webhookUrl());

                // Create transformer parameters
                Map<String, Object> transformerParams = new HashMap<>();
                transformerParams.put("webhookUrl", stub.webhookUrl());
                transformerParams.put("stubId", stub.id());

                // Add the transformer to the response with correct WireMock format
                response.put("transformers", Collections.singletonList("webhook-response-transformer"));
                response.put("transformerParameters", transformerParams);
            }

            mapping.put("response", response);

            // Register with WireMock
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(mapping, headers);

            wireMockAdminService.postWiremockMappings(entity);
            logger.info("Successfully registered SOAP stub {} with WireMock", stub.id());

        } catch (Exception e) {
            logger.error("Failed to register SOAP stub {} with WireMock: {}", stub.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to register SOAP stub with WireMock", e);
        }
    }

    /**
     * Deregister SOAP stub from WireMock
     */
    private void deregisterFromWireMock(SoapStub stub) {
        try {
            logger.info("Deregistering SOAP stub {} from WireMock", stub.id());

            wireMockAdminService.deleteWireMockMapping(stub.id());
            logger.info("Successfully deregistered SOAP stub {} from WireMock", stub.id());

        } catch (Exception e) {
            logger.error("Failed to deregister SOAP stub {} from WireMock: {}", stub.id(), e.getMessage());
            // Don't throw exception here as the database operation should still succeed
        }
    }
} 
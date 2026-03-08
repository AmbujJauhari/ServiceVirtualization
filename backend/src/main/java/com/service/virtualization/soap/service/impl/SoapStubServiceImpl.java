package com.service.virtualization.soap.service.impl;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.rest.service.WireMockAdminService;
import com.service.virtualization.soap.SoapStub;
import com.service.virtualization.soap.SoapStubRepository;
import com.service.virtualization.soap.service.SoapStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SOAP stub service implementation using WireMock infrastructure.
 * Only active when the soap-disabled profile is NOT active.
 */
@Service
@Profile("!soap-disabled")
public class SoapStubServiceImpl implements SoapStubService {

    private static final Logger logger = LoggerFactory.getLogger(SoapStubServiceImpl.class);

    @Autowired
    private final SoapStubRepository soapStubRepository;

    @Autowired
    private final WireMockAdminService wireMockAdminService;

    public SoapStubServiceImpl(SoapStubRepository soapStubRepository,
                               WireMockAdminService wireMockAdminService) {
        this.soapStubRepository = soapStubRepository;
        this.wireMockAdminService = wireMockAdminService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public SoapStub createStub(SoapStub stub) {
        logger.info("Creating SOAP stub: {}", stub.name());

        stub = new SoapStub(
                stub.id(), stub.name(), stub.description(), stub.userId(),
                stub.behindProxy(), stub.protocol(), stub.tags(), stub.status(),
                LocalDateTime.now(), LocalDateTime.now(), null,
                stub.url(), stub.soapAction(), stub.webhookUrl(),
                stub.matchConditions(), stub.response()
        );

        stub = soapStubRepository.save(stub);

        if (stub.status() == StubStatus.ACTIVE) {
            logger.info("Registering new active SOAP stub {} with WireMock", stub.id());
            String wiremockId = registerWithWireMock(stub);
            // Persist the WireMock mapping UUID so we can deregister it later
            stub = withWiremockId(stub, wiremockId);
            stub = soapStubRepository.save(stub);
        } else {
            logger.info("Creating inactive SOAP stub {}, skipping WireMock registration", stub.id());
        }

        return stub;
    }

    @Override
    public SoapStub updateStub(SoapStub stub) {
        logger.info("Updating SOAP stub: {}", stub.name());

        Optional<SoapStub> existingOpt = findStubById(stub.id());
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("SOAP stub not found with ID: " + stub.id());
        }
        SoapStub existing = existingOpt.get();

        stub = new SoapStub(
                stub.id(), stub.name(), stub.description(), stub.userId(),
                stub.behindProxy(), stub.protocol(), stub.tags(), stub.status(),
                existing.createdAt(), LocalDateTime.now(), existing.wiremockMappingId(),
                stub.url(), stub.soapAction(), stub.webhookUrl(),
                stub.matchConditions(), stub.response()
        );

        if (existing.status() == StubStatus.ACTIVE && stub.status() == StubStatus.INACTIVE) {
            logger.info("Deactivating SOAP stub {}, removing from WireMock", stub.id());
            deregisterFromWireMock(stub);
            stub = withWiremockId(stub, null);

        } else if (existing.status() == StubStatus.INACTIVE && stub.status() == StubStatus.ACTIVE) {
            logger.info("Activating SOAP stub {}, registering with WireMock", stub.id());
            String wiremockId = registerWithWireMock(stub);
            stub = withWiremockId(stub, wiremockId);

        } else if (stub.status() == StubStatus.ACTIVE) {
            logger.info("Updating active SOAP stub {}, re-registering with WireMock", stub.id());
            deregisterFromWireMock(stub);
            String wiremockId = registerWithWireMock(stub);
            stub = withWiremockId(stub, wiremockId);
        }

        return soapStubRepository.save(stub);
    }

    @Override
    public void deleteStub(String id) {
        logger.info("Deleting SOAP stub: {}", id);
        findStubById(id).ifPresent(stub -> {
            if (stub.status() == StubStatus.ACTIVE) {
                deregisterFromWireMock(stub);
            }
            soapStubRepository.deleteById(id);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    @Override public Optional<SoapStub> findStubById(String id) { return soapStubRepository.findById(id); }
    @Override public List<SoapStub>     findAllStubs()           { return soapStubRepository.findAll(); }
    @Override public List<SoapStub>     findStubsByUserId(String userId)        { return soapStubRepository.findByUserId(userId); }
    @Override public List<SoapStub>     findStubsByStatus(StubStatus status)    { return soapStubRepository.findByStatus(status); }
    @Override public List<SoapStub>     findStubsByUrl(String urlPattern)       { return soapStubRepository.findByUrlContaining(urlPattern); }

    @Override
    public SoapStub updateStubStatus(String id, StubStatus status) {
        logger.info("Updating SOAP stub {} status to {}", id, status);
        SoapStub existing = findStubById(id)
                .orElseThrow(() -> new IllegalArgumentException("SOAP stub not found with ID: " + id));
        return updateStub(existing.withStatus(status));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WireMock helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers the stub with WireMock and returns the WireMock mapping UUID.
     *
     * WireMock requires the mapping {@code id} to be a proper UUID (RFC 4122).
     * MongoDB ObjectIds are 24 hex chars (not a UUID) and are rejected with 422.
     * We generate a fresh UUID here and persist it as {@code wiremockMappingId}
     * on the stub so {@link #deregisterFromWireMock} can remove the right mapping.
     */
    private String registerWithWireMock(SoapStub stub) {
        try {
            String wiremockId = UUID.randomUUID().toString();
            logger.info("Registering SOAP stub {} with WireMock (mapping id: {})", stub.id(), wiremockId);

            Map<String, Object> mapping = new HashMap<>();
            mapping.put("id", wiremockId);
            mapping.put("priority", 1);

            // SOAP requests are always POST
            Map<String, Object> request = new HashMap<>();
            request.put("method", "POST");
            request.put("url", stub.url());

            // Optional SOAPAction header matching
            if (stub.soapAction() != null && !stub.soapAction().trim().isEmpty()) {
                Map<String, Object> headerMatchers = new HashMap<>();
                headerMatchers.put("SOAPAction", Map.of("equalTo", stub.soapAction()));
                request.put("headers", headerMatchers);
            }

            // Optional XML body matching
            if (stub.matchConditions().containsKey("body")) {
                String bodyContent  = (String) stub.matchConditions().get("body");
                String matchType    = (String) stub.matchConditions().getOrDefault("bodyMatchType", "equals");
                if (bodyContent != null && !bodyContent.trim().isEmpty()) {
                    Map<String, Object> bodyPattern = new HashMap<>();
                    switch (matchType.toLowerCase()) {
                        case "equals"  -> bodyPattern.put("equalTo",      bodyContent);
                        case "xpath"   -> bodyPattern.put("matchesXPath", bodyContent);
                        case "contains"-> bodyPattern.put("contains",     bodyContent);
                        case "regex",
                             "matches" -> bodyPattern.put("matches",      bodyContent);
                    }
                    request.put("bodyPatterns", Collections.singletonList(bodyPattern));
                }
            }
            mapping.put("request", request);

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("status", stub.response().getOrDefault("status", 200));
            response.put("headers", Map.of("Content-Type", "text/xml; charset=utf-8"));

            if (stub.response().containsKey("body")) {
                response.put("body", stub.response().get("body"));
            }

            // Webhook transformer
            if (stub.hasWebhook()) {
                logger.info("Adding webhook transformer for SOAP stub {} → {}", stub.id(), stub.webhookUrl());
                response.put("transformers", Collections.singletonList("webhook-response-transformer"));
                response.put("transformerParameters", Map.of(
                        "webhookUrl", stub.webhookUrl(),
                        "stubId",     stub.id()
                ));
            }
            mapping.put("response", response);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            wireMockAdminService.postWiremockMappings(new HttpEntity<>(mapping, headers));

            logger.info("Successfully registered SOAP stub {} with WireMock", stub.id());
            return wiremockId;

        } catch (Exception e) {
            logger.error("Failed to register SOAP stub {} with WireMock: {}", stub.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to register SOAP stub with WireMock", e);
        }
    }

    /**
     * Removes the WireMock mapping using the stored {@code wiremockMappingId} UUID.
     * Falls back gracefully if the mapping was never registered.
     */
    private void deregisterFromWireMock(SoapStub stub) {
        String mappingId = stub.wiremockMappingId();
        if (mappingId == null || mappingId.isBlank()) {
            logger.warn("SOAP stub {} has no WireMock mapping ID — nothing to deregister", stub.id());
            return;
        }
        try {
            logger.info("Deregistering SOAP stub {} from WireMock (mapping id: {})", stub.id(), mappingId);
            wireMockAdminService.deleteWireMockMapping(mappingId);
            logger.info("Successfully deregistered SOAP stub {} from WireMock", stub.id());
        } catch (Exception e) {
            logger.error("Failed to deregister SOAP stub {} from WireMock: {}", stub.id(), e.getMessage());
        }
    }

    /** Convenience — return a copy of the stub with the given wiremockMappingId. */
    private static SoapStub withWiremockId(SoapStub stub, String wiremockId) {
        return new SoapStub(
                stub.id(), stub.name(), stub.description(), stub.userId(),
                stub.behindProxy(), stub.protocol(), stub.tags(), stub.status(),
                stub.createdAt(), stub.updatedAt(), wiremockId,
                stub.url(), stub.soapAction(), stub.webhookUrl(),
                stub.matchConditions(), stub.response()
        );
    }
}

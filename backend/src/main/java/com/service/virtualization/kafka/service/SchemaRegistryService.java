package com.service.virtualization.kafka.service;

import com.service.virtualization.kafka.dto.SchemaInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.Base64Utils;
import org.springframework.context.annotation.Profile;

import java.util.*;

/**
 * Service for interacting with Confluent Schema Registry with RBAC support
 * Only active when kafka-disabled profile is NOT active
 */
@Service
@Profile("!kafka-disabled")
public class SchemaRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryService.class);

    @Value("${schema.registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${schema.registry.auth.type:basic}")
    private String authType;

    @Value("${schema.registry.auth.username:}")
    private String username;

    @Value("${schema.registry.auth.password:}")
    private String password;

    @Value("${schema.registry.auth.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get available schemas from Schema Registry
     */
    public List<SchemaInfoDTO> getAvailableSchemas() {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Get all subjects
            ResponseEntity<String[]> subjectsResponse = restTemplate.exchange(
                schemaRegistryUrl + "/subjects",
                HttpMethod.GET,
                entity,
                String[].class
            );

            if (subjectsResponse.getBody() == null) {
                return Collections.emptyList();
            }

            List<SchemaInfoDTO> schemas = new ArrayList<>();
            for (String subject : subjectsResponse.getBody()) {
                try {
                    // Get latest version for each subject
                    ResponseEntity<Map> latestVersionResponse = restTemplate.exchange(
                        schemaRegistryUrl + "/subjects/" + subject + "/versions/latest",
                        HttpMethod.GET,
                        entity,
                        Map.class
                    );

                    if (latestVersionResponse.getBody() != null) {
                        Map<String, Object> versionInfo = latestVersionResponse.getBody();
                        schemas.add(new SchemaInfoDTO(
                            subject,
                            (Integer) versionInfo.get("id"),
                            (Integer) versionInfo.get("version"),
                            (String) versionInfo.get("schema")
                        ));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to fetch latest version for subject: {}", subject, e);
                }
            }

            return schemas;

        } catch (Exception e) {
            logger.error("Failed to fetch schemas from Schema Registry", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get available versions for a schema subject
     */
    public List<Integer> getSchemaVersions(String subject) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Integer[]> response = restTemplate.exchange(
                schemaRegistryUrl + "/subjects/" + subject + "/versions",
                HttpMethod.GET,
                entity,
                Integer[].class
            );

            return response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();

        } catch (Exception e) {
            logger.error("Failed to fetch versions for subject: {}", subject, e);
            return Collections.emptyList();
        }
    }

    /**
     * Validate message against schema specified in headers
     * @param message The message content to validate
     * @param headers Message headers containing schema information
     * @return null if valid, error message if invalid
     */
    public String validateMessage(String message, Map<String, String> headers) {
        if (headers == null) {
            return "No schema information provided in headers";
        }

        try {
            String schemaId = headers.get("schema-id");
            String schemaSubject = headers.get("schema-subject");
            String schemaVersion = headers.getOrDefault("schema-version", "latest");

            String schemaContent = null;

            if (schemaId != null) {
                schemaContent = getSchemaById(Integer.parseInt(schemaId));
            } else if (schemaSubject != null) {
                schemaContent = getSchemaBySubjectVersion(schemaSubject, schemaVersion);
            } else {
                return "No schema ID or subject provided in headers";
            }

            if (schemaContent == null) {
                return "Schema not found in registry";
            }

            // Basic JSON validation for now
            // In a real implementation, you would use appropriate schema validation libraries
            if (schemaContent.contains("\"type\":\"record\"")) {
                // Basic Avro validation
                return validateAvroMessage(message, schemaContent);
            } else {
                // Basic JSON schema validation
                return validateJsonMessage(message, schemaContent);
            }

        } catch (Exception e) {
            logger.error("Schema validation error", e);
            return "Schema validation failed: " + e.getMessage();
        }
    }

    private String getSchemaById(Integer schemaId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                schemaRegistryUrl + "/schemas/ids/" + schemaId,
                HttpMethod.GET,
                entity,
                Map.class
            );

            return response.getBody() != null ? (String) response.getBody().get("schema") : null;

        } catch (Exception e) {
            logger.error("Failed to fetch schema by ID: {}", schemaId, e);
            return null;
        }
    }

    private String getSchemaBySubjectVersion(String subject, String version) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                schemaRegistryUrl + "/subjects/" + subject + "/versions/" + version,
                HttpMethod.GET,
                entity,
                Map.class
            );

            return response.getBody() != null ? (String) response.getBody().get("schema") : null;

        } catch (Exception e) {
            logger.error("Failed to fetch schema for subject: {} version: {}", subject, version, e);
            return null;
        }
    }

    private String validateAvroMessage(String message, String schema) {
        // Basic validation - in production, use proper Avro validation
        try {
            // Ensure message is valid JSON for Avro
            if (!message.trim().startsWith("{") || !message.trim().endsWith("}")) {
                return "Avro message must be valid JSON format";
            }
            return null; // Valid
        } catch (Exception e) {
            return "Invalid Avro message format: " + e.getMessage();
        }
    }

    private String validateJsonMessage(String message, String schema) {
        // Basic validation - in production, use proper JSON schema validation
        try {
            // Basic JSON syntax check
            if (!message.trim().startsWith("{") || !message.trim().endsWith("}")) {
                return "Message must be valid JSON format";
            }
            return null; // Valid
        } catch (Exception e) {
            return "Invalid JSON message format: " + e.getMessage();
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        switch (authType.toLowerCase()) {
            case "none":
                logger.debug("Using Schema Registry without authentication");
                // No authentication headers added
                break;
            case "basic":
                if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                    String auth = username + ":" + password;
                    byte[] encodedAuth = Base64Utils.encode(auth.getBytes());
                    String authHeader = "Basic " + new String(encodedAuth);
                    headers.set("Authorization", authHeader);
                    logger.debug("Using Schema Registry with Basic authentication for user: {}", username);
                } else {
                    logger.warn("Basic auth type specified but username/password not provided. Using no authentication.");
                }
                break;
            case "apikey":
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.set("Authorization", "Bearer " + apiKey);
                    logger.debug("Using Schema Registry with API Key authentication");
                } else {
                    logger.warn("API Key auth type specified but api-key not provided. Using no authentication.");
                }
                break;
            case "bearer":
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.set("Authorization", "Bearer " + apiKey);
                    logger.debug("Using Schema Registry with Bearer token authentication");
                } else {
                    logger.warn("Bearer auth type specified but api-key not provided. Using no authentication.");
                }
                break;
            default:
                logger.warn("Unknown auth type '{}'. Using no authentication.", authType);
                break;
        }

        return headers;
    }
} 
package com.service.virtualization.tibco.controller;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.tibco.model.TibcoStub;
import com.service.virtualization.tibco.service.TibcoStubService;
import com.service.virtualization.model.MessageHeader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * REST controller for managing TIBCO EMS stubs.
 * Only active when tibco-disabled profile is NOT active
 */
@RestController
@RequestMapping("/api/tibco/stubs")
@Tag(name = "TIBCO Stubs", description = "APIs for managing TIBCO EMS message stubs")
@Profile("!tibco-disabled")
public class TibcoStubController {
    private static final Logger logger = LoggerFactory.getLogger(TibcoStubController.class);

    private final TibcoStubService tibcoStubService;

    public TibcoStubController(TibcoStubService tibcoStubService) {
        this.tibcoStubService = tibcoStubService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new TIBCO stub",
            description = "Creates a new TIBCO EMS message stub",
            responses = {
                    @ApiResponse(responseCode = "201", description = "TIBCO stub created successfully",
                            content = @Content(schema = @Schema(implementation = TibcoStub.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request body")
            }
    )
    public ResponseEntity<TibcoStub> createStub(@RequestBody TibcoStub tibcoStub) {
        logger.debug("Creating TIBCO stub: {}", tibcoStub.getName());

        // Create the stub
        TibcoStub createdStub = tibcoStubService.create(tibcoStub);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdStub);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an existing TIBCO stub",
            description = "Updates an existing TIBCO EMS message stub",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TIBCO stub updated successfully",
                            content = @Content(schema = @Schema(implementation = TibcoStub.class))),
                    @ApiResponse(responseCode = "404", description = "TIBCO stub not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body")
            }
    )
    public ResponseEntity<TibcoStub> updateStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id,
            @RequestBody TibcoStub tibcoStub) {
        logger.debug("Updating TIBCO stub with ID: {}", id);

        try {
            // Update the stub
            TibcoStub updatedStub = tibcoStubService.update(id, tibcoStub);

            return ResponseEntity.ok(updatedStub);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating TIBCO stub: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a TIBCO stub by ID",
            description = "Retrieves a TIBCO EMS message stub by its ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TIBCO stub found",
                            content = @Content(schema = @Schema(implementation = TibcoStub.class))),
                    @ApiResponse(responseCode = "404", description = "TIBCO stub not found")
            }
    )
    public ResponseEntity<TibcoStub> getStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id) {
        logger.debug("Getting TIBCO stub with ID: {}", id);

        return tibcoStubService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
            summary = "Get all TIBCO stubs",
            description = "Retrieves all TIBCO EMS message stubs",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TIBCO stubs retrieved successfully",
                            content = @Content(schema = @Schema(implementation = TibcoStub.class)))
            }
    )
    public ResponseEntity<List<TibcoStub>> getAllStubs() {
        logger.debug("Getting all TIBCO stubs");

        List<TibcoStub> stubs = tibcoStubService.findAll();

        return ResponseEntity.ok(stubs);
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get TIBCO stubs by status",
            description = "Retrieves TIBCO EMS message stubs by status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TIBCO stubs retrieved successfully",
                            content = @Content(schema = @Schema(implementation = TibcoStub.class)))
            }
    )
    public ResponseEntity<List<TibcoStub>> getStubsByStatus(
            @Parameter(description = "Status (ACTIVE or INACTIVE)") @PathVariable String status) {
        logger.debug("Getting TIBCO stubs with status: {}", status);

        try {
            StubStatus stubStatus = StubStatus.valueOf(status.toUpperCase());
            List<TibcoStub> stubs = tibcoStubService.findByStatus(stubStatus);

            return ResponseEntity.ok(stubs);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get TIBCO stubs by user ID",
            description = "Retrieves TIBCO EMS message stubs by user ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TIBCO stubs retrieved successfully",
                            content = @Content(schema = @Schema(implementation = TibcoStub.class)))
            }
    )
    public ResponseEntity<List<TibcoStub>> getStubsByUserId(
            @Parameter(description = "User ID") @PathVariable String userId) {
        logger.debug("Getting TIBCO stubs for user: {}", userId);

        List<TibcoStub> stubs = tibcoStubService.findByUserId(userId);

        return ResponseEntity.ok(stubs);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a TIBCO stub",
            description = "Deletes a TIBCO EMS message stub by its ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "TIBCO stub deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "TIBCO stub not found")
            }
    )
    public ResponseEntity<Void> deleteStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id) {
        logger.debug("Deleting TIBCO stub with ID: {}", id);

        if (tibcoStubService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        tibcoStubService.delete(id);


        return ResponseEntity.noContent().build();
    }

    /**
     * Publish a message to a Tibco EMS queue or topic
     */
    @PostMapping("/publish")
    @Operation(
            summary = "Publish a message to Tibco EMS",
            description = "Publishes a message to a Tibco EMS queue or topic",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Message published successfully"),
                    @ApiResponse(responseCode = "500", description = "Failed to publish message")
            }
    )
    public ResponseEntity<Map<String, Object>> publishMessage(@RequestBody Map<String, Object> messageRequest) {
        try {
            String queueManager = "default"; // Hardcoded queue manager
            String destinationType = (String) messageRequest.getOrDefault("destinationType", "queue");
            String destinationName = (String) messageRequest.get("destinationName");
            String message = (String) messageRequest.get("message");
            List<Map<String, String>> headerMaps = (List<Map<String, String>>) messageRequest.getOrDefault("headers", new ArrayList<>());

            // Convert headers from maps to MessageHeader objects
            List<MessageHeader> headers = headerMaps.stream()
                    .map(map -> new MessageHeader(
                            map.get("name"),
                            map.get("value"),
                            map.getOrDefault("type", "string")))
                    .collect(Collectors.toList());

            logger.info("Publishing message to {} '{}' on Tibco EMS", destinationType, destinationName);

            boolean success = tibcoStubService.publishMessage(queueManager, destinationType, destinationName, message, headers);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Message published successfully to " + destinationType
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "success", false,
                        "message", "Failed to publish message to " + destinationType
                ));
            }
        } catch (Exception e) {
            logger.error("Error publishing message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error publishing message: " + e.getMessage()
            ));
        }
    }

} 
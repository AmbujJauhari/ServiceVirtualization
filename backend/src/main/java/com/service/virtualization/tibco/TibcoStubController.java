package com.service.virtualization.tibco;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing TIBCO EMS stubs.
 */
@RestController
@RequestMapping("/api/tibco/stubs")
@Tag(name = "TIBCO Stubs", description = "APIs for managing TIBCO EMS message stubs")
public class TibcoStubController {
    private static final Logger logger = LoggerFactory.getLogger(TibcoStubController.class);
    
    private final TibcoStubService tibcoStubService;
    private final TibcoStubListenerService tibcoStubListenerService;

    public TibcoStubController(TibcoStubService tibcoStubService, TibcoStubListenerService tibcoStubListenerService) {
        this.tibcoStubService = tibcoStubService;
        this.tibcoStubListenerService = tibcoStubListenerService;
    }

    @PostMapping
    @Operation(
        summary = "Create a new TIBCO stub",
        description = "Creates a new TIBCO EMS message stub",
        responses = {
            @ApiResponse(responseCode = "201", description = "TIBCO stub created successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
        }
    )
    public ResponseEntity<TibcoStubDTO> createStub(@RequestBody TibcoStubDTO tibcoStubDTO) {
        logger.debug("Creating TIBCO stub: {}", tibcoStubDTO.name());
        
        // Convert DTO to domain model
        TibcoStub tibcoStub = toTibcoStub(tibcoStubDTO);
        
        // Create the stub
        TibcoStub createdStub = tibcoStubService.createStub(tibcoStub);
        
        // Refresh listeners if stub is active
        if ("ACTIVE".equals(createdStub.getStatus())) {
            tibcoStubListenerService.refreshListeners();
        }
        
        // Convert back to DTO and return
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fromTibcoStub(createdStub));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing TIBCO stub",
        description = "Updates an existing TIBCO EMS message stub",
        responses = {
            @ApiResponse(responseCode = "200", description = "TIBCO stub updated successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class))),
            @ApiResponse(responseCode = "404", description = "TIBCO stub not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
        }
    )
    public ResponseEntity<TibcoStubDTO> updateStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id,
            @RequestBody TibcoStubDTO tibcoStubDTO) {
        logger.debug("Updating TIBCO stub with ID: {}", id);
        
        // Convert DTO to domain model
        TibcoStub tibcoStub = toTibcoStub(tibcoStubDTO);
        
        try {
            // Update the stub
            TibcoStub updatedStub = tibcoStubService.updateStub(id, tibcoStub);
            
            // Refresh listeners if stub is active
            if ("ACTIVE".equals(updatedStub.getStatus())) {
                tibcoStubListenerService.refreshListeners();
            }
            
            // Convert back to DTO and return
            return ResponseEntity.ok(fromTibcoStub(updatedStub));
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
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class))),
            @ApiResponse(responseCode = "404", description = "TIBCO stub not found")
        }
    )
    public ResponseEntity<TibcoStubDTO> getStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id) {
        logger.debug("Getting TIBCO stub with ID: {}", id);
        
        return tibcoStubService.getStubById(id)
                .map(tibcoStub -> ResponseEntity.ok(fromTibcoStub(tibcoStub)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
        summary = "Get all TIBCO stubs",
        description = "Retrieves all TIBCO EMS message stubs",
        responses = {
            @ApiResponse(responseCode = "200", description = "TIBCO stubs retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class)))
        }
    )
    public ResponseEntity<List<TibcoStubDTO>> getAllStubs() {
        logger.debug("Getting all TIBCO stubs");
        
        List<TibcoStub> stubs = tibcoStubService.getAllStubs();
        List<TibcoStubDTO> stubDTOs = stubs.stream()
                .map(this::fromTibcoStub)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get TIBCO stubs by status",
        description = "Retrieves TIBCO EMS message stubs by status",
        responses = {
            @ApiResponse(responseCode = "200", description = "TIBCO stubs retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class)))
        }
    )
    public ResponseEntity<List<TibcoStubDTO>> getStubsByStatus(
            @Parameter(description = "Status (ACTIVE or INACTIVE)") @PathVariable String status) {
        logger.debug("Getting TIBCO stubs with status: {}", status);
        
        List<TibcoStub> stubs = tibcoStubService.getStubsByStatus(status);
        List<TibcoStubDTO> stubDTOs = stubs.stream()
                .map(this::fromTibcoStub)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
    }

    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get TIBCO stubs by user ID",
        description = "Retrieves TIBCO EMS message stubs by user ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "TIBCO stubs retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class)))
        }
    )
    public ResponseEntity<List<TibcoStubDTO>> getStubsByUserId(
            @Parameter(description = "User ID") @PathVariable String userId) {
        logger.debug("Getting TIBCO stubs for user: {}", userId);
        
        List<TibcoStub> stubs = tibcoStubService.getStubsByUserId(userId);
        List<TibcoStubDTO> stubDTOs = stubs.stream()
                .map(this::fromTibcoStub)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
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
        
        if (tibcoStubService.getStubById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        tibcoStubService.deleteStub(id);
        
        // Refresh listeners to remove deleted stub
        tibcoStubListenerService.refreshListeners();
        
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @Operation(
        summary = "Activate a TIBCO stub",
        description = "Activates a TIBCO EMS message stub by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "TIBCO stub activated successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class))),
            @ApiResponse(responseCode = "404", description = "TIBCO stub not found")
        }
    )
    public ResponseEntity<TibcoStubDTO> activateStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id) {
        logger.debug("Activating TIBCO stub with ID: {}", id);
        
        try {
            TibcoStub activatedStub = tibcoStubService.activateStub(id);
            
            // Refresh listeners to add the newly activated stub
            tibcoStubListenerService.refreshListeners();
            
            return ResponseEntity.ok(fromTibcoStub(activatedStub));
        } catch (IllegalArgumentException e) {
            logger.error("Error activating TIBCO stub: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate a TIBCO stub",
        description = "Deactivates a TIBCO EMS message stub by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "TIBCO stub deactivated successfully", 
                        content = @Content(schema = @Schema(implementation = TibcoStubDTO.class))),
            @ApiResponse(responseCode = "404", description = "TIBCO stub not found")
        }
    )
    public ResponseEntity<TibcoStubDTO> deactivateStub(
            @Parameter(description = "TIBCO stub ID") @PathVariable String id) {
        logger.debug("Deactivating TIBCO stub with ID: {}", id);
        
        try {
            TibcoStub deactivatedStub = tibcoStubService.deactivateStub(id);
            
            // Refresh listeners to remove the deactivated stub
            tibcoStubListenerService.refreshListeners();
            
            return ResponseEntity.ok(fromTibcoStub(deactivatedStub));
        } catch (IllegalArgumentException e) {
            logger.error("Error deactivating TIBCO stub: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Converts a TibcoStubDTO to a TibcoStub domain model.
     *
     * @param dto The DTO to convert
     * @return The domain model
     */
    private TibcoStub toTibcoStub(TibcoStubDTO dto) {
        TibcoDestination requestDestination = new TibcoDestination(
                dto.requestDestination().type(),
                dto.requestDestination().name()
        );

        TibcoDestination responseDestination = new TibcoDestination(
                dto.responseDestination().type(),
                dto.responseDestination().name()
        );
        
        // Convert body match criteria
        List<TibcoStub.BodyMatchCriteria> bodyMatchCriteriaList = new ArrayList<>();
        if (dto.bodyMatchCriteria() != null) {
            for (TibcoStubDTO.BodyMatchCriteriaDTO criteriaDTO : dto.bodyMatchCriteria()) {
                TibcoStub.BodyMatchCriteria criteria = new TibcoStub.BodyMatchCriteria(
                    criteriaDTO.type(),
                    criteriaDTO.expression(),
                    criteriaDTO.value(),
                    criteriaDTO.operator()
                );
                bodyMatchCriteriaList.add(criteria);
            }
        }
        
        TibcoStub tibcoStub = new TibcoStub();
        tibcoStub.setId(dto.id());
        tibcoStub.setName(dto.name());
        tibcoStub.setDescription(dto.description());
        tibcoStub.setUserId(dto.userId());
        tibcoStub.setRequestDestination(requestDestination);
        tibcoStub.setResponseDestination(responseDestination);
        tibcoStub.setMessageSelector(dto.messageSelector());
        tibcoStub.setBodyMatchCriteria(bodyMatchCriteriaList);
        tibcoStub.setResponseType(dto.responseType());
        tibcoStub.setResponseContent(dto.responseContent());
        tibcoStub.setResponseHeaders(dto.responseHeaders());
        tibcoStub.setStatus(dto.status());
        tibcoStub.setCreatedAt(dto.createdAt());
        tibcoStub.setUpdatedAt(dto.updatedAt());
        
        return tibcoStub;
    }
    
    /**
     * Converts a TibcoStub domain model to a TibcoStubDTO.
     *
     * @param tibcoStub The domain model to convert
     * @return The DTO
     */
    private TibcoStubDTO fromTibcoStub(TibcoStub tibcoStub) {
        TibcoStubDTO.TibcoDestinationDTO requestDestinationDTO = new TibcoStubDTO.TibcoDestinationDTO(
                tibcoStub.getRequestDestination().getType(),
                tibcoStub.getRequestDestination().getName()
        );

        TibcoStubDTO.TibcoDestinationDTO responseDestinationDTO = new TibcoStubDTO.TibcoDestinationDTO(
                tibcoStub.getResponseDestination().getType(),
                tibcoStub.getResponseDestination().getName()
        );
        
        // Convert body match criteria
        List<TibcoStubDTO.BodyMatchCriteriaDTO> bodyMatchCriteriaDTOList = new ArrayList<>();
        if (tibcoStub.getBodyMatchCriteria() != null) {
            for (TibcoStub.BodyMatchCriteria criteria : tibcoStub.getBodyMatchCriteria()) {
                TibcoStubDTO.BodyMatchCriteriaDTO criteriaDTO = new TibcoStubDTO.BodyMatchCriteriaDTO(
                    criteria.getType(),
                    criteria.getExpression(),
                    criteria.getValue(),
                    criteria.getOperator()
                );
                bodyMatchCriteriaDTOList.add(criteriaDTO);
            }
        }
        
        return new TibcoStubDTO(
                tibcoStub.getId(),
                tibcoStub.getName(),
                tibcoStub.getDescription(),
                tibcoStub.getUserId(),
                requestDestinationDTO,
                responseDestinationDTO,
                tibcoStub.getMessageSelector(),
                bodyMatchCriteriaDTOList,
                tibcoStub.getResponseType(),
                tibcoStub.getResponseContent(),
                tibcoStub.getResponseHeaders(),
                tibcoStub.getStatus(),
                tibcoStub.getCreatedAt(),
                tibcoStub.getUpdatedAt()
        );
    }
} 
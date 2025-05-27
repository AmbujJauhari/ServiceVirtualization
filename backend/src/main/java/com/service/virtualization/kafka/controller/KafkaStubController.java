package com.service.virtualization.kafka.controller;

import com.service.virtualization.kafka.KafkaStubService;
import com.service.virtualization.kafka.dto.KafkaStubDTO;
import com.service.virtualization.kafka.mapper.KafkaStubMapper;
import com.service.virtualization.kafka.model.KafkaStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API for managing Kafka stubs
 */
@RestController
@RequestMapping("/api/kafka/stubs")
public class KafkaStubController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStubController.class);

    private final KafkaStubService kafkaStubService;
    private final KafkaStubMapper kafkaStubMapper;

    @Autowired
    public KafkaStubController(KafkaStubService kafkaStubService, KafkaStubMapper kafkaStubMapper) {
        this.kafkaStubService = kafkaStubService;
        this.kafkaStubMapper = kafkaStubMapper;
    }

    /**
     * Create a new Kafka stub
     *
     * @param kafkaStubDTO The Kafka stub data
     * @return The created Kafka stub
     */
    @PostMapping
    public ResponseEntity<KafkaStubDTO> createKafkaStub(@RequestBody KafkaStubDTO kafkaStubDTO) {
        logger.info("Creating Kafka stub: {}", kafkaStubDTO.name());
        
        KafkaStub kafkaStub = kafkaStubMapper.toModel(kafkaStubDTO);
        KafkaStub createdStub = kafkaStubService.createStub(kafkaStub);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kafkaStubMapper.toDTO(createdStub));
    }

    /**
     * Get all Kafka stubs
     *
     * @return List of all Kafka stubs
     */
    @GetMapping
    public ResponseEntity<List<KafkaStubDTO>> getAllKafkaStubs() {
        logger.info("Fetching all Kafka stubs");
        
        List<KafkaStub> stubs = kafkaStubService.getAllStubs();
        List<KafkaStubDTO> stubDTOs = stubs.stream()
                .map(kafkaStubMapper::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
    }

    /**
     * Get a Kafka stub by ID
     *
     * @param id The ID of the Kafka stub to retrieve
     * @return The Kafka stub with the specified ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<KafkaStubDTO> getKafkaStubById(@PathVariable String id) {
        logger.info("Fetching Kafka stub with ID: {}", id);
        
        Optional<KafkaStub> kafkaStub = kafkaStubService.getStubById(id);
        if (kafkaStub.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(kafkaStubMapper.toDTO(kafkaStub.orElse(null)));
    }

    /**
     * Update an existing Kafka stub
     *
     * @param id The ID of the Kafka stub to update
     * @param kafkaStubDTO The updated Kafka stub data
     * @return The updated Kafka stub
     */
    @PutMapping("/{id}")
    public ResponseEntity<KafkaStubDTO> updateKafkaStub(@PathVariable String id, 
                                                     @RequestBody KafkaStubDTO kafkaStubDTO) {
        logger.info("Updating Kafka stub with ID: {}", id);
        
        if (!id.equals(kafkaStubDTO.id())) {
            return ResponseEntity.badRequest().build();
        }
        
        KafkaStub kafkaStub = kafkaStubMapper.toModel(kafkaStubDTO);
        KafkaStub updatedStub = kafkaStubService.updateStub(id, kafkaStub);
        
        return ResponseEntity.ok(kafkaStubMapper.toDTO(updatedStub));
    }

    /**
     * Delete a Kafka stub
     *
     * @param id The ID of the Kafka stub to delete
     * @return No content on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKafkaStub(@PathVariable String id) {
        logger.info("Deleting Kafka stub with ID: {}", id);
        
        kafkaStubService.deleteStub(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle the status of a Kafka stub
     *
     * @param id The ID of the Kafka stub to toggle
     * @return The updated Kafka stub with the toggled status
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<KafkaStubDTO> toggleKafkaStub(@PathVariable String id) {
        logger.info("Toggling status of Kafka stub with ID: {}", id);
        
        KafkaStub toggledStub = kafkaStubService.toggleStubStatus(id);
        return ResponseEntity.ok(kafkaStubMapper.toDTO(toggledStub));
    }
} 
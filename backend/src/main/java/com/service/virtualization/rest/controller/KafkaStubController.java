package com.service.virtualization.rest.controller;

import com.service.virtualization.kafka.KafkaStubDTO;
import com.service.virtualization.kafka.KafkaStub;
import com.service.virtualization.kafka.KafkaStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kafka/stubs")
public class KafkaStubController {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStubController.class);
    
    private final KafkaStubService kafkaStubService;
    
    public KafkaStubController(KafkaStubService kafkaStubService) {
        this.kafkaStubService = kafkaStubService;
    }
    
    @GetMapping
    public ResponseEntity<List<KafkaStubDTO>> getAllStubs(@RequestParam(required = false) String userId) {
        logger.debug("Getting all Kafka stubs");
        
        List<KafkaStub> stubs;
        if (userId != null && !userId.isEmpty()) {
            stubs = kafkaStubService.getStubsByUserId(userId);
        } else {
            stubs = kafkaStubService.getAllStubs();
        }
        
        List<KafkaStubDTO> stubDTOs = stubs.stream()
                .map(this::toKafkaStubDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<KafkaStubDTO> getStubById(@PathVariable Long id) {
        logger.debug("Getting Kafka stub with id: {}", id);
        
        return kafkaStubService.getStubById(id)
                .map(stub -> ResponseEntity.ok(toKafkaStubDTO(stub)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<KafkaStubDTO> createStub(@RequestBody KafkaStubDTO kafkaStubDTO) {
        logger.debug("Creating Kafka stub: {}", kafkaStubDTO.name());
        
        KafkaStub stub = toKafkaStub(kafkaStubDTO);
        KafkaStub createdStub = kafkaStubService.createStub(stub);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toKafkaStubDTO(createdStub));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<KafkaStubDTO> updateStub(@PathVariable Long id, @RequestBody KafkaStubDTO kafkaStubDTO) {
        logger.debug("Updating Kafka stub with id: {}", id);
        
        KafkaStub stub = toKafkaStub(kafkaStubDTO);
        KafkaStub updatedStub = kafkaStubService.updateStub(id, stub);
        
        return ResponseEntity.ok(toKafkaStubDTO(updatedStub));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStub(@PathVariable Long id) {
        logger.debug("Deleting Kafka stub with id: {}", id);
        
        kafkaStubService.deleteStub(id);
        
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<KafkaStubDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        logger.debug("Updating status of Kafka stub with id: {} to {}", id, status);
        
        KafkaStub updatedStub = kafkaStubService.updateStatus(id, status);
        
        return ResponseEntity.ok(toKafkaStubDTO(updatedStub));
    }
    
    @GetMapping("/topic/{topic}/producer")
    public ResponseEntity<List<KafkaStubDTO>> getActiveProducerStubsByTopic(@PathVariable String topic) {
        logger.debug("Getting active producer stubs for topic: {}", topic);
        
        List<KafkaStub> stubs = kafkaStubService.getActiveProducerStubsByTopic(topic);
        
        List<KafkaStubDTO> stubDTOs = stubs.stream()
                .map(this::toKafkaStubDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
    }
    
    @GetMapping("/topic/{topic}/consumer")
    public ResponseEntity<List<KafkaStubDTO>> getActiveConsumerStubsByTopic(@PathVariable String topic) {
        logger.debug("Getting active consumer stubs for topic: {}", topic);
        
        List<KafkaStub> stubs = kafkaStubService.getActiveConsumerStubsByTopic(topic);
        
        List<KafkaStubDTO> stubDTOs = stubs.stream()
                .map(this::toKafkaStubDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stubDTOs);
    }
    
    // Convert domain model to DTO
    private KafkaStubDTO toKafkaStubDTO(KafkaStub stub) {
        return new KafkaStubDTO(
            stub.getId(),
            stub.getName(),
            stub.getDescription(),
            stub.getUserId(),
            stub.getTopic(),
            stub.getKeyPattern(),
            stub.getValuePattern(),
            stub.getActiveForProducer(),
            stub.getActiveForConsumer(),
            stub.getResponseContent(),
            stub.getLatency(),
            stub.getStatus(),
            stub.getCreatedAt(),
            stub.getUpdatedAt()
        );
    }
    
    // Convert DTO to domain model
    private KafkaStub toKafkaStub(KafkaStubDTO dto) {
        KafkaStub stub = new KafkaStub();
        stub.setId(dto.id());
        stub.setName(dto.name());
        stub.setDescription(dto.description());
        stub.setUserId(dto.userId());
        stub.setTopic(dto.topic());
        stub.setKeyPattern(dto.keyPattern());
        stub.setValuePattern(dto.valuePattern());
        stub.setActiveForProducer(dto.activeForProducer());
        stub.setActiveForConsumer(dto.activeForConsumer());
        stub.setResponseContent(dto.responseContent());
        stub.setLatency(dto.latency());
        stub.setStatus(dto.status());
        stub.setCreatedAt(dto.createdAt());
        stub.setUpdatedAt(dto.updatedAt());
        return stub;
    }
} 
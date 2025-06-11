package com.service.virtualization.kafka;

import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.repository.KafkaStubRepository;
import com.service.virtualization.kafka.service.KafkaTopicService;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.soap.SoapStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Kafka stubs
 * Only active when kafka-disabled profile is NOT active
 */
@Service
@Profile("!kafka-disabled")
public class KafkaStubService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStubService.class);

    private final KafkaStubRepository kafkaStubRepository;
    private final KafkaTopicService kafkaTopicService;

    public KafkaStubService(KafkaStubRepository kafkaStubRepository, KafkaTopicService kafkaTopicService) {
        this.kafkaStubRepository = kafkaStubRepository;
        this.kafkaTopicService = kafkaTopicService;
    }

    public List<KafkaStub> getAllStubs() {
        logger.debug("Getting all Kafka stubs");
        return kafkaStubRepository.findAll();
    }

    public Optional<KafkaStub> getStubById(String id) {
        logger.debug("Getting Kafka stub with id: {}", id);
        return kafkaStubRepository.findById(id);
    }

    public List<KafkaStub> getStubsByUserId(String userId) {
        logger.debug("Getting Kafka stubs for user: {}", userId);
        return kafkaStubRepository.findAllByUserId(userId);
    }

    public KafkaStub createStub(KafkaStub kafkaStub) {
        logger.debug("Creating Kafka stub: {}", kafkaStub.name());
        
        // Auto-create topics if they don't exist
        try {
            ensureTopicsExist(kafkaStub);
        } catch (Exception e) {
            logger.error("Failed to create topics for stub: {}", kafkaStub.name(), e);
            throw new RuntimeException("Failed to create required topics: " + e.getMessage(), e);
        }
        
        // Create new stub with updated timestamps and null ID for auto-generation
        KafkaStub newStub = new KafkaStub(
            null, // Let database generate ID
            kafkaStub.name(),
            kafkaStub.description(),
            kafkaStub.userId(),
            kafkaStub.requestTopic(),
            kafkaStub.responseTopic(),
            kafkaStub.requestContentFormat(),
            kafkaStub.responseContentFormat(),
            kafkaStub.requestContentMatcher(),
            kafkaStub.keyMatchType(),
            kafkaStub.keyPattern(),
            kafkaStub.contentMatchType(),
            kafkaStub.valuePattern(),
            kafkaStub.contentPattern(),
            kafkaStub.caseSensitive(),
            kafkaStub.responseType(),
            kafkaStub.responseKey(),
            kafkaStub.responseContent(),
            kafkaStub.useResponseSchemaRegistry(),
            kafkaStub.responseSchemaId(),
            kafkaStub.responseSchemaSubject(),
            kafkaStub.responseSchemaVersion(),
            kafkaStub.latency(),
            kafkaStub.status(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            kafkaStub.callbackUrl(),
            kafkaStub.callbackHeaders(),
            kafkaStub.tags()
        );
        
        return kafkaStubRepository.save(newStub);
    }

    public KafkaStub updateStubStatus(String id, StubStatus status) {
        logger.info("Updating Kafka stub {} status to {}", id, status);

        Optional<KafkaStub> stubOpt = getStubById(id);
        if (stubOpt.isEmpty()) {
            throw new IllegalArgumentException("Kafka stub not found with ID: " + id);
        }

        KafkaStub existingStub = stubOpt.get();
        KafkaStub updatedStub = new KafkaStub(
            existingStub.id(),
            existingStub.name(),
            existingStub.description(),
            existingStub.userId(),
            existingStub.requestTopic(),
            existingStub.responseTopic(),
            existingStub.requestContentFormat(),
            existingStub.responseContentFormat(),
            existingStub.requestContentMatcher(),
            existingStub.keyMatchType(),
            existingStub.keyPattern(),
            existingStub.contentMatchType(),
            existingStub.valuePattern(),
            existingStub.contentPattern(),
            existingStub.caseSensitive(),
            existingStub.responseType(),
            existingStub.responseKey(),
            existingStub.responseContent(),
            existingStub.useResponseSchemaRegistry(),
            existingStub.responseSchemaId(),
            existingStub.responseSchemaSubject(),
            existingStub.responseSchemaVersion(),
            existingStub.latency(),
            status,
            existingStub.createdAt(),
            LocalDateTime.now(),
            existingStub.callbackUrl(),
            existingStub.callbackHeaders(),
            existingStub.tags()
        );

        return kafkaStubRepository.save(updatedStub);
    }

    public KafkaStub updateStub(String id, KafkaStub kafkaStub) {
        logger.debug("Updating Kafka stub with id: {}", id);

        // Check if stub exists
        if (!kafkaStubRepository.existsById(id)) {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }

        // Auto-create topics if they don't exist (in case topics were changed)
        try {
            ensureTopicsExist(kafkaStub);
        } catch (Exception e) {
            logger.error("Failed to create topics for stub update: {}", kafkaStub.name(), e);
            throw new RuntimeException("Failed to create required topics: " + e.getMessage(), e);
        }

        // Create updated stub with preserved creation time and updated timestamp
        Optional<KafkaStub> existingStubOpt = kafkaStubRepository.findById(id);
        LocalDateTime createdAt = existingStubOpt.map(KafkaStub::createdAt).orElse(LocalDateTime.now());
        
        KafkaStub updatedStub = new KafkaStub(
            id,
            kafkaStub.name(),
            kafkaStub.description(),
            kafkaStub.userId(),
            kafkaStub.requestTopic(),
            kafkaStub.responseTopic(),
            kafkaStub.requestContentFormat(),
            kafkaStub.responseContentFormat(),
            kafkaStub.requestContentMatcher(),
            kafkaStub.keyMatchType(),
            kafkaStub.keyPattern(),
            kafkaStub.contentMatchType(),
            kafkaStub.valuePattern(),
            kafkaStub.contentPattern(),
            kafkaStub.caseSensitive(),
            kafkaStub.responseType(),
            kafkaStub.responseKey(),
            kafkaStub.responseContent(),
            kafkaStub.useResponseSchemaRegistry(),
            kafkaStub.responseSchemaId(),
            kafkaStub.responseSchemaSubject(),
            kafkaStub.responseSchemaVersion(),
            kafkaStub.latency(),
            kafkaStub.status(),
            createdAt,
            LocalDateTime.now(),
            kafkaStub.callbackUrl(),
            kafkaStub.callbackHeaders(),
            kafkaStub.tags()
        );
        
        return kafkaStubRepository.save(updatedStub);
    }

    public void deleteStub(String id) {
        logger.debug("Deleting Kafka stub with id: {}", id);

        // Check if stub exists before deleting
        if (!kafkaStubRepository.existsById(id)) {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }

        kafkaStubRepository.deleteById(id);
    }

    public KafkaStub updateStatus(String id, StubStatus status) {
        logger.debug("Updating status of Kafka stub with id: {} to {}", id, status);
        return kafkaStubRepository.updateStatus(id, status);
    }

    public List<KafkaStub> getActiveStubsByRequestTopic(String topic) {
        logger.debug("Getting active stubs for request topic: {}", topic);
        return kafkaStubRepository.findActiveStubsByRequestTopic(topic);
    }

    public KafkaStub toggleStubStatus(String id) {
        logger.debug("Toggling status of Kafka stub with id: {}", id);
        KafkaStub stub = kafkaStubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kafka stub not found with id: " + id));

        StubStatus newStatus = StubStatus.ACTIVE.equals(stub.status()) ? StubStatus.INACTIVE : StubStatus.ACTIVE;
        
        KafkaStub updatedStub = new KafkaStub(
            stub.id(),
            stub.name(),
            stub.description(),
            stub.userId(),
            stub.requestTopic(),
            stub.responseTopic(),
            stub.requestContentFormat(),
            stub.responseContentFormat(),
            stub.requestContentMatcher(),
            stub.keyMatchType(),
            stub.keyPattern(),
            stub.contentMatchType(),
            stub.valuePattern(),
            stub.contentPattern(),
            stub.caseSensitive(),
            stub.responseType(),
            stub.responseKey(),
            stub.responseContent(),
            stub.useResponseSchemaRegistry(),
            stub.responseSchemaId(),
            stub.responseSchemaSubject(),
            stub.responseSchemaVersion(),
            stub.latency(),
            newStatus,
            stub.createdAt(),
            LocalDateTime.now(),
            stub.callbackUrl(),
            stub.callbackHeaders(),
            stub.tags()
        );

        return kafkaStubRepository.save(updatedStub);
    }

    /**
     * Ensure that the topics referenced by the stub exist, creating them if necessary
     *
     * @param kafkaStub the stub containing topic references
     */
    private void ensureTopicsExist(KafkaStub kafkaStub) {
        logger.debug("Ensuring topics exist for stub: {}", kafkaStub.name());
        
        // Collect unique topic names
        String requestTopic = kafkaStub.requestTopic();
        String responseTopic = kafkaStub.responseTopic();
        
        if (requestTopic != null && !requestTopic.trim().isEmpty()) {
            logger.debug("Ensuring request topic exists: {}", requestTopic);
            kafkaTopicService.createTopicIfNotExists(requestTopic.trim());
        }
        
        // Only create response topic if it's different from request topic
        if (responseTopic != null && 
            !responseTopic.trim().isEmpty() && 
            !responseTopic.trim().equals(requestTopic)) {
            logger.debug("Ensuring response topic exists: {}", responseTopic);
            kafkaTopicService.createTopicIfNotExists(responseTopic.trim());
        }
    }
} 
package com.service.virtualization.kafka;

import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.repository.KafkaStubRepository;
import com.service.virtualization.kafka.service.KafkaTopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
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
        logger.debug("Creating Kafka stub: {}", kafkaStub.getName());
        
        // Auto-create topics if they don't exist
        try {
            ensureTopicsExist(kafkaStub);
        } catch (Exception e) {
            logger.error("Failed to create topics for stub: {}", kafkaStub.getName(), e);
            throw new RuntimeException("Failed to create required topics: " + e.getMessage(), e);
        }
        
        kafkaStub.setId(null); // Ensure we're creating a new stub (let database generate ID)
        kafkaStub.setCreatedAt(LocalDateTime.now());
        kafkaStub.setUpdatedAt(LocalDateTime.now());
        
        return kafkaStubRepository.save(kafkaStub);
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
            logger.error("Failed to create topics for stub update: {}", kafkaStub.getName(), e);
            throw new RuntimeException("Failed to create required topics: " + e.getMessage(), e);
        }

        kafkaStub.setId(id);
        kafkaStub.setUpdatedAt(LocalDateTime.now());
        return kafkaStubRepository.save(kafkaStub);
    }

    public void deleteStub(String id) {
        logger.debug("Deleting Kafka stub with id: {}", id);

        // Check if stub exists before deleting
        if (!kafkaStubRepository.existsById(id)) {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }

        kafkaStubRepository.deleteById(id);
    }

    public KafkaStub updateStatus(String id, String status) {
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

        String newStatus = "ACTIVE".equals(stub.getStatus()) ? "INACTIVE" : "ACTIVE";
        stub.setStatus(newStatus);
        stub.setUpdatedAt(LocalDateTime.now());

        return kafkaStubRepository.save(stub);
    }

    /**
     * Ensure that the topics referenced by the stub exist, creating them if necessary
     *
     * @param kafkaStub the stub containing topic references
     */
    private void ensureTopicsExist(KafkaStub kafkaStub) {
        logger.debug("Ensuring topics exist for stub: {}", kafkaStub.getName());
        
        // Collect unique topic names
        String requestTopic = kafkaStub.getRequestTopic();
        String responseTopic = kafkaStub.getResponseTopic();
        
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
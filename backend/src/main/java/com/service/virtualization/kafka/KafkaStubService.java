package com.service.virtualization.kafka;

import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.repository.KafkaStubRepository;
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

    public KafkaStubService(KafkaStubRepository kafkaStubRepository) {
        this.kafkaStubRepository = kafkaStubRepository;
    }

    public List<KafkaStub> getAllStubs() {
        logger.debug("Getting all Kafka stubs");
        return kafkaStubRepository.findAll();
    }

    public Optional<KafkaStub> getStubById(Long id) {
        logger.debug("Getting Kafka stub with id: {}", id);
        return kafkaStubRepository.findById(id);
    }

    public List<KafkaStub> getStubsByUserId(String userId) {
        logger.debug("Getting Kafka stubs for user: {}", userId);
        return kafkaStubRepository.findAllByUserId(userId);
    }

    public KafkaStub createStub(KafkaStub kafkaStub) {
        logger.debug("Creating Kafka stub: {}", kafkaStub.getName());
        kafkaStub.setId(null); // Ensure we're creating a new stub
        kafkaStub.setCreatedAt(LocalDateTime.now());
        kafkaStub.setUpdatedAt(LocalDateTime.now());
        return kafkaStubRepository.save(kafkaStub);
    }

    public KafkaStub updateStub(Long id, KafkaStub kafkaStub) {
        logger.debug("Updating Kafka stub with id: {}", id);

        // Check if stub exists
        if (!kafkaStubRepository.existsById(id)) {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }

        kafkaStub.setId(id);
        kafkaStub.setUpdatedAt(LocalDateTime.now());
        return kafkaStubRepository.save(kafkaStub);
    }

    public void deleteStub(Long id) {
        logger.debug("Deleting Kafka stub with id: {}", id);

        // Check if stub exists before deleting
        if (!kafkaStubRepository.existsById(id)) {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }

        kafkaStubRepository.deleteById(id);
    }

    public KafkaStub updateStatus(Long id, String status) {
        logger.debug("Updating status of Kafka stub with id: {} to {}", id, status);
        return kafkaStubRepository.updateStatus(id, status);
    }

    public List<KafkaStub> getActiveProducerStubsByTopic(String topic) {
        logger.debug("Getting active producer stubs for topic: {}", topic);
        return kafkaStubRepository.findActiveProducerStubsByTopic(topic);
    }

    public List<KafkaStub> getActiveConsumerStubsByTopic(String topic) {
        logger.debug("Getting active consumer stubs for topic: {}", topic);
        return kafkaStubRepository.findActiveConsumerStubsByTopic(topic);
    }

    public KafkaStub toggleStubStatus(Long id) {
        logger.debug("Toggling status of Kafka stub with id: {}", id);
        KafkaStub stub = kafkaStubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kafka stub not found with id: " + id));

        String newStatus = "ACTIVE".equals(stub.getStatus()) ? "INACTIVE" : "ACTIVE";
        stub.setStatus(newStatus);
        stub.setUpdatedAt(LocalDateTime.now());

        return kafkaStubRepository.save(stub);
    }
} 
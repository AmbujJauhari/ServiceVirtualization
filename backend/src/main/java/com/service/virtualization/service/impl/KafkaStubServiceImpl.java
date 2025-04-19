package com.service.virtualization.service.impl;

import com.service.virtualization.model.KafkaStub;
import com.service.virtualization.repository.KafkaStubRepository;
import com.service.virtualization.service.KafkaStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KafkaStubServiceImpl implements KafkaStubService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStubServiceImpl.class);
    
    private final KafkaStubRepository kafkaStubRepository;
    
    public KafkaStubServiceImpl(KafkaStubRepository kafkaStubRepository) {
        this.kafkaStubRepository = kafkaStubRepository;
    }

    @Override
    public List<KafkaStub> getAllStubs() {
        logger.debug("Getting all Kafka stubs");
        return kafkaStubRepository.findAll();
    }

    @Override
    public Optional<KafkaStub> getStubById(Long id) {
        logger.debug("Getting Kafka stub with id: {}", id);
        return kafkaStubRepository.findById(id);
    }

    @Override
    public List<KafkaStub> getStubsByUserId(String userId) {
        logger.debug("Getting Kafka stubs for user: {}", userId);
        return kafkaStubRepository.findAllByUserId(userId);
    }

    @Override
    public KafkaStub createStub(KafkaStub kafkaStub) {
        logger.debug("Creating Kafka stub: {}", kafkaStub.getName());
        kafkaStub.setId(null); // Ensure we're creating a new stub
        kafkaStub.setCreatedAt(LocalDateTime.now());
        kafkaStub.setUpdatedAt(LocalDateTime.now());
        return kafkaStubRepository.save(kafkaStub);
    }

    @Override
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

    @Override
    public void deleteStub(Long id) {
        logger.debug("Deleting Kafka stub with id: {}", id);
        
        // Check if stub exists before deleting
        if (!kafkaStubRepository.existsById(id)) {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }
        
        kafkaStubRepository.deleteById(id);
    }

    @Override
    public KafkaStub updateStatus(Long id, String status) {
        logger.debug("Updating status of Kafka stub with id: {} to {}", id, status);
        return kafkaStubRepository.updateStatus(id, status);
    }

    @Override
    public List<KafkaStub> getActiveProducerStubsByTopic(String topic) {
        logger.debug("Getting active producer stubs for topic: {}", topic);
        return kafkaStubRepository.findActiveProducerStubsByTopic(topic);
    }

    @Override
    public List<KafkaStub> getActiveConsumerStubsByTopic(String topic) {
        logger.debug("Getting active consumer stubs for topic: {}", topic);
        return kafkaStubRepository.findActiveConsumerStubsByTopic(topic);
    }
} 
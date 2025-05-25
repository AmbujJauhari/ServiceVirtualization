package com.service.virtualization.kafka.repository;

import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.model.StubStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Kafka stubs
 */
public interface KafkaStubRepository {
    
    /**
     * Find all Kafka stubs
     * 
     * @return list of all Kafka stubs
     */
    List<KafkaStub> findAll();
    
    /**
     * Find Kafka stub by id
     * 
     * @param id the stub id
     * @return the stub if found
     */
    Optional<KafkaStub> findById(Long id);
    
    /**
     * Find Kafka stubs by user id
     * 
     * @param userId the user id
     * @return list of stubs for the user
     */
    List<KafkaStub> findAllByUserId(String userId);
    
    /**
     * Find active Kafka stubs for producers by topic
     * 
     * @param topic the topic
     * @return list of active stubs for the topic
     */
    List<KafkaStub> findActiveProducerStubsByTopic(String topic);
    
    /**
     * Find active Kafka stubs for consumers by topic
     * 
     * @param topic the topic
     * @return list of active stubs for the topic
     */
    List<KafkaStub> findActiveConsumerStubsByTopic(String topic);
    
    /**
     * Save a Kafka stub (create new or update existing)
     * 
     * @param kafkaStub the stub to save
     * @return the saved stub
     */
    KafkaStub save(KafkaStub kafkaStub);
    
    /**
     * Delete a Kafka stub
     * 
     * @param id the stub id to delete
     */
    void deleteById(Long id);
    
    /**
     * Update status of a Kafka stub
     * 
     * @param id the stub id
     * @param status the new status
     * @return the updated stub
     */
    KafkaStub updateStatus(Long id, String status);
    
    /**
     * Check if a Kafka stub exists by id
     * 
     * @param id the stub id
     * @return true if the stub exists
     */
    boolean existsById(Long id);
    
    /**
     * Find stubs by topic and status
     * 
     * @param topic the topic
     * @param status the status
     * @return list of matching stubs
     */
    List<KafkaStub> findAllByTopicAndStatus(String topic, String status);
    
    /**
     * Find stubs by status and producer flag
     * 
     * @param status the status
     * @param activeForProducer the producer flag
     * @return list of matching stubs
     */
    List<KafkaStub> findAllByStatusAndActiveForProducer(String status, Boolean activeForProducer);
    
    /**
     * Find stubs by status and consumer flag
     * 
     * @param status the status
     * @param activeForConsumer the consumer flag
     * @return list of matching stubs
     */
    List<KafkaStub> findAllByStatusAndActiveForConsumer(String status, Boolean activeForConsumer);

    List<KafkaStub> findByUserId(String userId);
    List<KafkaStub> findByUserIdAndStatus(String userId, StubStatus status);
    List<KafkaStub> findByTopic(String topic);
    List<KafkaStub> findByTopicAndStatus(String topic, StubStatus status);
    Optional<KafkaStub> findById(String id);
    void delete(KafkaStub stub);
} 
package com.service.virtualization.kafka;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing Kafka stubs
 */
public interface KafkaStubService {
    
    /**
     * Get all Kafka stubs
     * 
     * @return list of all stubs
     */
    List<KafkaStub> getAllStubs();
    
    /**
     * Get Kafka stub by id
     * 
     * @param id the stub id
     * @return the stub if found
     */
    Optional<KafkaStub> getStubById(Long id);
    
    /**
     * Get Kafka stubs by user id
     * 
     * @param userId the user id
     * @return list of stubs for the user
     */
    List<KafkaStub> getStubsByUserId(String userId);
    
    /**
     * Create a new Kafka stub
     * 
     * @param kafkaStub the stub to create
     * @return the created stub
     */
    KafkaStub createStub(KafkaStub kafkaStub);
    
    /**
     * Update an existing Kafka stub
     * 
     * @param id the stub id
     * @param kafkaStub the stub to update
     * @return the updated stub
     */
    KafkaStub updateStub(Long id, KafkaStub kafkaStub);
    
    /**
     * Delete a Kafka stub
     * 
     * @param id the stub id
     */
    void deleteStub(Long id);
    
    /**
     * Update status of a Kafka stub
     * 
     * @param id the stub id
     * @param status the new status
     * @return the updated stub
     */
    KafkaStub updateStatus(Long id, String status);
    
    /**
     * Find active producer stubs for a specific topic
     * 
     * @param topic the topic
     * @return list of active producer stubs
     */
    List<KafkaStub> getActiveProducerStubsByTopic(String topic);
    
    /**
     * Find active consumer stubs for a specific topic
     * 
     * @param topic the topic
     * @return list of active consumer stubs
     */
    List<KafkaStub> getActiveConsumerStubsByTopic(String topic);
    
    /**
     * Toggle the status of a Kafka stub
     * 
     * @param id the stub id
     * @return the updated stub
     */
    KafkaStub toggleStubStatus(Long id);
} 
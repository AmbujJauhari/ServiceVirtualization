package com.service.virtualization.ibmmq;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for IBMMQStub entities
 */
@Repository
public interface IBMMQStubRepository extends MongoRepository<IBMMQStub, String> {
    
    /**
     * Find IBM MQ stubs by user ID
     * @param userId the user ID
     * @return list of IBM MQ stubs
     */
    List<IBMMQStub> findByUserId(String userId);
    
    /**
     * Find active IBM MQ stubs by user ID
     * @param userId the user ID
     * @param status the status
     * @return list of active IBM MQ stubs
     */
    List<IBMMQStub> findByUserIdAndStatus(String userId, boolean status);
    
    /**
     * Find IBM MQ stubs by queue name
     * @param queueName the queue name
     * @return list of IBM MQ stubs
     */
    List<IBMMQStub> findByQueueName(String queueName);
    
    /**
     * Find IBM MQ stubs by queue manager and queue name
     * @param queueManager the queue manager
     * @param queueName the queue name
     * @return list of IBM MQ stubs
     */
    List<IBMMQStub> findByQueueManagerAndQueueName(String queueManager, String queueName);
    
    /**
     * Find active IBM MQ stubs by queue manager and queue name
     * @param queueManager the queue manager
     * @param queueName the queue name
     * @param status the status
     * @return list of active IBM MQ stubs
     */
    List<IBMMQStub> findByQueueManagerAndQueueNameAndStatus(String queueManager, String queueName, boolean status);
} 
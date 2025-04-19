package com.service.virtualization.ibmmq;

import com.service.virtualization.model.MessageHeader;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for IBMMQStub operations
 */
public interface IBMMQStubService {
    
    /**
     * Create a new IBM MQ stub
     * @param ibmMQStub the IBM MQ stub to create
     * @return the created IBM MQ stub
     */
    IBMMQStub create(IBMMQStub ibmMQStub);
    
    /**
     * Find all IBM MQ stubs
     * @return list of all IBM MQ stubs
     */
    List<IBMMQStub> findAll();
    
    /**
     * Find IBM MQ stubs by user ID
     * @param userId the user ID
     * @return list of IBM MQ stubs
     */
    List<IBMMQStub> findByUserId(String userId);
    
    /**
     * Find active IBM MQ stubs by user ID
     * @param userId the user ID
     * @return list of active IBM MQ stubs
     */
    List<IBMMQStub> findActiveByUserId(String userId);
    
    /**
     * Find IBM MQ stub by ID
     * @param id the IBM MQ stub ID
     * @return the IBM MQ stub if found
     */
    Optional<IBMMQStub> findById(String id);
    
    /**
     * Update an existing IBM MQ stub
     * @param id the IBM MQ stub ID
     * @param ibmMQStub the updated IBM MQ stub
     * @return the updated IBM MQ stub
     */
    IBMMQStub update(String id, IBMMQStub ibmMQStub);
    
    /**
     * Delete an IBM MQ stub by ID
     * @param id the IBM MQ stub ID
     */
    void delete(String id);
    
    /**
     * Update IBM MQ stub status
     * @param id the IBM MQ stub ID
     * @param status the new status
     * @return the updated IBM MQ stub
     */
    IBMMQStub updateStatus(String id, boolean status);
    
    /**
     * Add a message header to an IBM MQ stub
     * @param stubId the IBM MQ stub ID
     * @param header the message header to add
     * @return the updated IBM MQ stub
     */
    IBMMQStub addHeader(String stubId, MessageHeader header);
    
    /**
     * Remove a message header from an IBM MQ stub
     * @param stubId the IBM MQ stub ID
     * @param headerName the name of the header to remove
     * @return the updated IBM MQ stub
     */
    IBMMQStub removeHeader(String stubId, String headerName);
    
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
    List<IBMMQStub> findByQueueManagerAndName(String queueManager, String queueName);
    
    /**
     * Find active IBM MQ stubs by queue manager and queue name
     * @param queueManager the queue manager
     * @param queueName the queue name
     * @return list of active IBM MQ stubs
     */
    List<IBMMQStub> findActiveByQueueManagerAndName(String queueManager, String queueName);
    
    /**
     * Publish a message to IBM MQ
     * @param queueManager the queue manager
     * @param queueName the queue name
     * @param message the message content
     * @param headers optional message headers
     * @return true if message was sent successfully
     */
    boolean publishMessage(String queueManager, String queueName, String message, List<MessageHeader> headers);
} 
package com.service.virtualization.service;

import com.service.virtualization.model.ActiveMQStub;
import com.service.virtualization.model.MessageHeader;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for ActiveMQStub operations
 */
public interface ActiveMQStubService {
    
    /**
     * Create a new ActiveMQ stub
     * @param activeMQStub the ActiveMQ stub to create
     * @return the created ActiveMQ stub
     */
    ActiveMQStub create(ActiveMQStub activeMQStub);
    
    /**
     * Find all ActiveMQ stubs
     * @return list of all ActiveMQ stubs
     */
    List<ActiveMQStub> findAll();
    
    /**
     * Find ActiveMQ stubs by user ID
     * @param userId the user ID
     * @return list of ActiveMQ stubs
     */
    List<ActiveMQStub> findByUserId(String userId);
    
    /**
     * Find active ActiveMQ stubs by user ID
     * @param userId the user ID
     * @return list of active ActiveMQ stubs
     */
    List<ActiveMQStub> findActiveByUserId(String userId);
    
    /**
     * Find ActiveMQ stub by ID
     * @param id the ActiveMQ stub ID
     * @return the ActiveMQ stub if found
     */
    Optional<ActiveMQStub> findById(String id);
    
    /**
     * Update an existing ActiveMQ stub
     * @param id the ActiveMQ stub ID
     * @param activeMQStub the updated ActiveMQ stub
     * @return the updated ActiveMQ stub
     */
    ActiveMQStub update(String id, ActiveMQStub activeMQStub);
    
    /**
     * Delete an ActiveMQ stub by ID
     * @param id the ActiveMQ stub ID
     */
    void delete(String id);
    
    /**
     * Update ActiveMQ stub status
     * @param id the ActiveMQ stub ID
     * @param status the new status
     * @return the updated ActiveMQ stub
     */
    ActiveMQStub updateStatus(String id, boolean status);
    
    /**
     * Add a message header to an ActiveMQ stub
     * @param stubId the ActiveMQ stub ID
     * @param header the message header to add
     * @return the updated ActiveMQ stub
     */
    ActiveMQStub addHeader(String stubId, MessageHeader header);
    
    /**
     * Remove a message header from an ActiveMQ stub
     * @param stubId the ActiveMQ stub ID
     * @param headerName the name of the header to remove
     * @return the updated ActiveMQ stub
     */
    ActiveMQStub removeHeader(String stubId, String headerName);
    
    /**
     * Find ActiveMQ stubs by destination name
     * @param destinationName the destination name
     * @return list of ActiveMQ stubs
     */
    List<ActiveMQStub> findByDestinationName(String destinationName);
    
    /**
     * Find ActiveMQ stubs by destination type and name
     * @param destinationType the destination type
     * @param destinationName the destination name
     * @return list of ActiveMQ stubs
     */
    List<ActiveMQStub> findByDestinationTypeAndName(String destinationType, String destinationName);
    
    /**
     * Find active ActiveMQ stubs by destination type and name
     * @param destinationType the destination type
     * @param destinationName the destination name
     * @return list of active ActiveMQ stubs
     */
    List<ActiveMQStub> findActiveByDestinationTypeAndName(String destinationType, String destinationName);
    
    /**
     * Publish a message to ActiveMQ
     * @param destinationType the destination type (queue or topic)
     * @param destinationName the destination name
     * @param message the message content
     * @param headers optional message headers
     * @return true if message was sent successfully
     */
    boolean publishMessage(String destinationType, String destinationName, String message, List<MessageHeader> headers);
} 
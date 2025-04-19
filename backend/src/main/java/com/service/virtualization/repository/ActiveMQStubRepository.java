package com.service.virtualization.repository;

import com.service.virtualization.model.ActiveMQStub;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ActiveMQStub entities
 */
@Repository
public interface ActiveMQStubRepository extends MongoRepository<ActiveMQStub, String> {
    
    /**
     * Find ActiveMQ stubs by user ID
     * @param userId the user ID
     * @return list of ActiveMQ stubs
     */
    List<ActiveMQStub> findByUserId(String userId);
    
    /**
     * Find active ActiveMQ stubs by user ID
     * @param userId the user ID
     * @param status the status
     * @return list of active ActiveMQ stubs
     */
    List<ActiveMQStub> findByUserIdAndStatus(String userId, boolean status);
    
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
    List<ActiveMQStub> findByDestinationTypeAndDestinationName(String destinationType, String destinationName);
    
    /**
     * Find active ActiveMQ stubs by destination type and name
     * @param destinationType the destination type
     * @param destinationName the destination name
     * @param status the status
     * @return list of active ActiveMQ stubs
     */
    List<ActiveMQStub> findByDestinationTypeAndDestinationNameAndStatus(String destinationType, String destinationName, boolean status);
} 
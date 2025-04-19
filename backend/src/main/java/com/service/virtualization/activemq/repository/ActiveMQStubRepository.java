package com.service.virtualization.activemq.repository;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.model.StubStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ActiveMQ stub CRUD operations.
 */
@Repository
public interface ActiveMQStubRepository extends MongoRepository<ActiveMQStub, String> {
    
    /**
     * Find stubs by status.
     *
     * @param status The status to filter by
     * @return List of stubs with the specified status
     */
    List<ActiveMQStub> findByStatus(StubStatus status);
    
    /**
     * Find stubs by user ID.
     *
     * @param userId The user ID to filter by
     * @return List of stubs belonging to the specified user
     */
    List<ActiveMQStub> findByUserId(String userId);
    
    /**
     * Find stubs by user ID and status.
     *
     * @param userId The user ID to filter by
     * @param status The status to filter by
     * @return List of stubs with the specified status belonging to the specified user
     */
    List<ActiveMQStub> findByUserIdAndStatus(String userId, StubStatus status);
    
    /**
     * Find stubs by destination name.
     *
     * @param destinationName The destination name to filter by
     * @return List of stubs with the specified destination name
     */
    List<ActiveMQStub> findByDestinationName(String destinationName);
    
    /**
     * Find stubs by destination name and type with priority greater than the given value.
     *
     * @param destinationName The destination name to filter by
     * @param destinationType The destination type to filter by
     * @param priority The minimum priority to filter by
     * @return List of stubs matching the criteria
     */
    List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThan(
            String destinationName, String destinationType, int priority);
    
    /**
     * Find stubs by destination name and type with priority greater than or equal to the given value.
     *
     * @param destinationName The destination name to filter by
     * @param destinationType The destination type to filter by
     * @param priority The minimum priority to filter by (inclusive)
     * @return List of stubs matching the criteria
     */
    List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThanEqual(
            String destinationName, String destinationType, int priority);
            
    /**
     * Find the highest priority among stubs with the specified destination name and type.
     * 
     * @param destinationName The destination name to filter by
     * @param destinationType The destination type to filter by
     * @return The stub with the highest priority, if any
     */
    ActiveMQStub findFirstByDestinationNameAndDestinationTypeOrderByPriorityDesc(
            String destinationName, String destinationType);
} 
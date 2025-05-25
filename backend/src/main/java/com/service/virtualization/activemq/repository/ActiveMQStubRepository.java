package com.service.virtualization.activemq.repository;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.model.StubStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ActiveMQ stub CRUD operations.
 */
public interface ActiveMQStubRepository {
    
    List<ActiveMQStub> findByStatus(StubStatus status);
    
    List<ActiveMQStub> findByUserId(String userId);
    
    List<ActiveMQStub> findByUserIdAndStatus(String userId, StubStatus status);
    
    List<ActiveMQStub> findByDestinationName(String destinationName);
    
    List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThan(
            String destinationName, String destinationType, int priority);
    
    List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThanEqual(
            String destinationName, String destinationType, int priority);
            
    ActiveMQStub findFirstByDestinationNameAndDestinationTypeOrderByPriorityDesc(
            String destinationName, String destinationType);

    ActiveMQStub save(ActiveMQStub stub);

    void delete(ActiveMQStub stub);

    Optional<ActiveMQStub> findById(String id);

    List<ActiveMQStub> findAll();
} 
package com.service.virtualization.activemq.repository.mongodb;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.repository.ActiveMQStubRepository;
import com.service.virtualization.model.StubStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongodb")
public class MongoActiveMQStubRepository implements ActiveMQStubRepository {
    
    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "activeMQStubs";

    public MongoActiveMQStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<ActiveMQStub> findByStatus(StubStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, ActiveMQStub.class, COLLECTION_NAME);
    }
    
    @Override
    public List<ActiveMQStub> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, ActiveMQStub.class, COLLECTION_NAME);
    }
    
    @Override
    public List<ActiveMQStub> findByUserIdAndStatus(String userId, StubStatus status) {
        Query query = new Query(Criteria.where("userId").is(userId).and("status").is(status));
        return mongoTemplate.find(query, ActiveMQStub.class, COLLECTION_NAME);
    }
    
    @Override
    public List<ActiveMQStub> findByDestinationName(String destinationName) {
        Query query = new Query(Criteria.where("destinationName").is(destinationName));
        return mongoTemplate.find(query, ActiveMQStub.class, COLLECTION_NAME);
    }
    
    @Override
    public List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThan(
            String destinationName, String destinationType, int priority) {
        Query query = new Query(Criteria.where("destinationName").is(destinationName)
            .and("destinationType").is(destinationType)
            .and("priority").gt(priority));
        return mongoTemplate.find(query, ActiveMQStub.class, COLLECTION_NAME);
    }
    
    @Override
    public List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThanEqual(
            String destinationName, String destinationType, int priority) {
        Query query = new Query(Criteria.where("destinationName").is(destinationName)
            .and("destinationType").is(destinationType)
            .and("priority").gte(priority));
        return mongoTemplate.find(query, ActiveMQStub.class, COLLECTION_NAME);
    }
            
    @Override
    public ActiveMQStub findFirstByDestinationNameAndDestinationTypeOrderByPriorityDesc(
            String destinationName, String destinationType) {
        Query query = new Query(Criteria.where("destinationName").is(destinationName)
            .and("destinationType").is(destinationType));
        query.limit(1);
        return mongoTemplate.findOne(query, ActiveMQStub.class, COLLECTION_NAME);
    }

    @Override
    public ActiveMQStub save(ActiveMQStub stub) {
        return mongoTemplate.save(stub, COLLECTION_NAME);
    }

    @Override
    public void delete(ActiveMQStub stub) {
        mongoTemplate.remove(stub, COLLECTION_NAME);
    }

    @Override
    public Optional<ActiveMQStub> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, ActiveMQStub.class, COLLECTION_NAME));
    }

    @Override
    public List<ActiveMQStub> findAll() {
        return mongoTemplate.findAll(ActiveMQStub.class, COLLECTION_NAME);
    }
} 
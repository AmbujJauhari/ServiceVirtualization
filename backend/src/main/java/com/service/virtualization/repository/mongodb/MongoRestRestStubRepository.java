package com.service.virtualization.repository.mongodb;

import com.service.virtualization.model.RestStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.repository.RestStubRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of StubRepository
 */
@Repository
@Profile("mongodb")
public class MongoRestRestStubRepository implements RestStubRepository {

    private final MongoTemplate mongoTemplate;
    
    public MongoRestRestStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public RestStub save(RestStub stub) {
        return mongoTemplate.save(stub);
    }
    
    @Override
    public Optional<RestStub> findById(String id) {
        RestStub stub = mongoTemplate.findById(id, RestStub.class);
        return Optional.ofNullable(stub);
    }
    
    @Override
    public List<RestStub> findAll() {
        return mongoTemplate.findAll(RestStub.class);
    }
    
    @Override
    public List<RestStub> findByStatus(StubStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, RestStub.class);
    }
    
    @Override
    public List<RestStub> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, RestStub.class);
    }
    
    @Override
    public List<RestStub> findByServicePath(String path) {
        Query query = new Query(Criteria.where("servicePath").is(path));
        return mongoTemplate.find(query, RestStub.class);
    }
    
    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, RestStub.class);
    }
    
    @Override
    public void delete(RestStub stub) {
        mongoTemplate.remove(stub);
    }
    
    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, RestStub.class);
    }
} 
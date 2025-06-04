package com.service.virtualization.ibmmq.repository.mongodb;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.repository.IBMMQStubRepository;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.tibco.model.TibcoStub;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongodb")
public class MongoIBMMQStubRepository implements IBMMQStubRepository {
    private static final String COLLECTION_NAME = "ibmmq_stubs";
    private final MongoTemplate mongoTemplate;

    public MongoIBMMQStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<IBMMQStub> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, IBMMQStub.class, COLLECTION_NAME);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, TibcoStub.class, COLLECTION_NAME);
    }

    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, TibcoStub.class, COLLECTION_NAME);
    }


    @Override
    public Optional<IBMMQStub> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, IBMMQStub.class, COLLECTION_NAME));
    }

    @Override
    public IBMMQStub save(IBMMQStub stub) {
        return mongoTemplate.save(stub, COLLECTION_NAME);
    }

    @Override
    public List<IBMMQStub> findAll() {
        return mongoTemplate.findAll(IBMMQStub.class, COLLECTION_NAME);
    }

    @Override
    public List<IBMMQStub> findByStatus(StubStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, IBMMQStub.class, COLLECTION_NAME);
    }
} 
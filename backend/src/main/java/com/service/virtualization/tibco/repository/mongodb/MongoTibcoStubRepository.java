package com.service.virtualization.tibco.repository.mongodb;

import com.service.virtualization.tibco.model.TibcoStub;
import com.service.virtualization.tibco.repository.TibcoStubRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongodb")
public class MongoTibcoStubRepository implements TibcoStubRepository {
    private static final String COLLECTION_NAME = "tibco_stubs";
    private final MongoTemplate mongoTemplate;

    public MongoTibcoStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public TibcoStub save(TibcoStub tibcoStub) {
        return mongoTemplate.save(tibcoStub, COLLECTION_NAME);
    }

    @Override
    public Optional<TibcoStub> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, TibcoStub.class, COLLECTION_NAME));
    }

    @Override
    public List<TibcoStub> findAll() {
        return mongoTemplate.findAll(TibcoStub.class, COLLECTION_NAME);
    }

    @Override
    public List<TibcoStub> findByStatus(String status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, TibcoStub.class, COLLECTION_NAME);
    }

    @Override
    public List<TibcoStub> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, TibcoStub.class, COLLECTION_NAME);
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
} 
package com.service.virtualization.soap;

import com.service.virtualization.model.StubStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB implementation of the SoapStubRepository interface
 */
@Repository
@Profile("mongodb")
public class MongoSoapStubRepository implements SoapStubRepository {

    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "soap_stubs";

    public MongoSoapStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SoapStub save(SoapStub stub) {
        return mongoTemplate.save(stub, COLLECTION_NAME);
    }

    @Override
    public Optional<SoapStub> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, SoapStub.class, COLLECTION_NAME));
    }

    @Override
    public List<SoapStub> findAll() {
        return mongoTemplate.findAll(SoapStub.class, COLLECTION_NAME);
    }

    @Override
    public List<SoapStub> findByStatus(StubStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, SoapStub.class, COLLECTION_NAME);
    }

    @Override
    public List<SoapStub> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, SoapStub.class, COLLECTION_NAME);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, SoapStub.class, COLLECTION_NAME);
    }

    @Override
    public void delete(SoapStub stub) {
        if (stub.id() != null) {
            deleteById(stub.id());
        }
    }

    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.exists(query, SoapStub.class, COLLECTION_NAME);
    }

    @Override
    public List<SoapStub> findByUrlContaining(String urlPattern) {
        Query query = new Query(Criteria.where("url").regex(urlPattern, "i"));
        return mongoTemplate.find(query, SoapStub.class, COLLECTION_NAME);
    }
} 
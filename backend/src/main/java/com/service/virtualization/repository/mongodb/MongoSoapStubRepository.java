package com.service.virtualization.repository.mongodb;

import com.service.virtualization.model.SoapStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.repository.SoapStubRepository;
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
        // Generate ID if not present
        if (stub.id() == null) {
            stub = new SoapStub(
                UUID.randomUUID().toString(),
                stub.name(),
                stub.description(),
                stub.userId(),
                stub.behindProxy(),
                stub.protocol(),
                stub.tags(),
                stub.status(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                stub.wiremockMappingId(),
                stub.wsdlUrl(),
                stub.serviceName(),
                stub.portName(),
                stub.operationName(),
                stub.matchConditions(),
                stub.response()
            );
        }
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
    public List<SoapStub> findByServiceName(String serviceName) {
        Query query = new Query(Criteria.where("serviceName").is(serviceName));
        return mongoTemplate.find(query, SoapStub.class, COLLECTION_NAME);
    }

    @Override
    public List<SoapStub> findByOperationName(String operationName) {
        Query query = new Query(Criteria.where("operationName").is(operationName));
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
} 
package com.service.virtualization.kafka.repository.mongodb;

import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.kafka.repository.KafkaStubRepository;
import com.service.virtualization.model.StubStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mongodb")
public class MongoKafkaStubRepository implements KafkaStubRepository {
    private static final String COLLECTION_NAME = "kafka_stubs";
    private final MongoTemplate mongoTemplate;

    public MongoKafkaStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<KafkaStub> findAll() {
        return mongoTemplate.findAll(KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public Optional<KafkaStub> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, KafkaStub.class, COLLECTION_NAME));
    }

    @Override
    public List<KafkaStub> findAllByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public List<KafkaStub> findActiveStubsByRequestTopic(String topic) {
        Query query = new Query(Criteria.where("requestTopic").is(topic)
                .and("status").is(StubStatus.ACTIVE));
        return mongoTemplate.find(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public KafkaStub save(KafkaStub kafkaStub) {
        return mongoTemplate.save(kafkaStub, COLLECTION_NAME);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public KafkaStub updateStatus(String id, StubStatus status) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().set("status", status).set("updatedAt", java.time.LocalDateTime.now());
        
        mongoTemplate.updateFirst(query, update, KafkaStub.class, COLLECTION_NAME);
        
        return findById(id).orElseThrow(() -> new RuntimeException("Kafka stub not found with id: " + id));
    }

    @Override
    public List<KafkaStub> findAllByTopicAndStatus(String topic, StubStatus status) {
        Query query = new Query(Criteria.where("requestTopic").is(topic).and("status").is(status));
        return mongoTemplate.find(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public List<KafkaStub> findByUserId(String userId) {
        return findAllByUserId(userId);
    }

    @Override
    public List<KafkaStub> findByUserIdAndStatus(String userId, StubStatus status) {
        Query query = new Query(Criteria.where("userId").is(userId).and("status").is(status));
        return mongoTemplate.find(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public List<KafkaStub> findByTopic(String topic) {
        Query query = new Query(Criteria.where("topic").is(topic));
        return mongoTemplate.find(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public List<KafkaStub> findByTopicAndStatus(String topic, StubStatus status) {
        Query query = new Query(Criteria.where("topic").is(topic).and("status").is(status));
        return mongoTemplate.find(query, KafkaStub.class, COLLECTION_NAME);
    }

    @Override
    public void delete(KafkaStub stub) {
        mongoTemplate.remove(stub, COLLECTION_NAME);
    }
} 
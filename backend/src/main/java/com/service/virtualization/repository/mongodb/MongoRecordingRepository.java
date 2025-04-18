package com.service.virtualization.repository.mongodb;

import com.service.virtualization.model.Recording;
import com.service.virtualization.repository.RecordingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of RecordingRepository
 * Uses MongoTemplate to interact with MongoDB
 */
@Repository
@Profile("mongodb")
public class MongoRecordingRepository implements RecordingRepository {
    
    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "recordings";
    
    public MongoRecordingRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public Recording save(Recording recording) {
        // MongoDB will automatically generate an ID if it's null
        return mongoTemplate.save(recording, COLLECTION_NAME);
    }
    
    @Override
    public Optional<Recording> findById(String id) {
        Recording recording = mongoTemplate.findById(id, Recording.class, COLLECTION_NAME);
        return Optional.ofNullable(recording);
    }
    
    @Override
    public List<Recording> findAll() {
        return mongoTemplate.findAll(Recording.class, COLLECTION_NAME);
    }
    
    @Override
    public List<Recording> findBySessionId(String sessionId) {
        Query query = new Query(Criteria.where("sessionId").is(sessionId));
        return mongoTemplate.find(query, Recording.class, COLLECTION_NAME);
    }
    
    @Override
    public List<Recording> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, Recording.class, COLLECTION_NAME);
    }
    
    @Override
    public List<Recording> findByServicePath(String path) {
        Query query = new Query(Criteria.where("requestData.path").is(path));
        return mongoTemplate.find(query, Recording.class, COLLECTION_NAME);
    }
    
    @Override
    public List<Recording> findByConvertedToStubFalse() {
        Query query = new Query(Criteria.where("convertedToStub").is(false));
        return mongoTemplate.find(query, Recording.class, COLLECTION_NAME);
    }
    
    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, COLLECTION_NAME);
    }
    
    @Override
    public void delete(Recording recording) {
        if (recording != null && recording.id() != null) {
            deleteById(recording.id());
        }
    }
    
    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, COLLECTION_NAME);
    }
} 
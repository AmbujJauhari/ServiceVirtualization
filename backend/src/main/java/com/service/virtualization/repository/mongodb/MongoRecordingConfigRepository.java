package com.service.virtualization.repository.mongodb;

import com.service.virtualization.model.RecordingConfig;
import com.service.virtualization.repository.RecordingConfigRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of RecordingConfigRepository
 */
@Repository
@Profile("mongodb")
public class MongoRecordingConfigRepository implements RecordingConfigRepository {
    
    private static final String COLLECTION_NAME = "recording_configs";
    
    private final MongoTemplate mongoTemplate;
    
    public MongoRecordingConfigRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public RecordingConfig save(RecordingConfig config) {
        return mongoTemplate.save(config, COLLECTION_NAME);
    }
    
    @Override
    public Optional<RecordingConfig> findById(String id) {
        RecordingConfig config = mongoTemplate.findById(id, RecordingConfig.class, COLLECTION_NAME);
        return Optional.ofNullable(config);
    }
    
    @Override
    public List<RecordingConfig> findAll() {
        return mongoTemplate.findAll(RecordingConfig.class, COLLECTION_NAME);
    }
    
    @Override
    public List<RecordingConfig> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, RecordingConfig.class, COLLECTION_NAME);
    }
    
    @Override
    public List<RecordingConfig> findByActiveTrue() {
        Query query = new Query(Criteria.where("active").is(true));
        return mongoTemplate.find(query, RecordingConfig.class, COLLECTION_NAME);
    }
    
    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, RecordingConfig.class, COLLECTION_NAME);
    }
    
    @Override
    public void delete(RecordingConfig config) {
        mongoTemplate.remove(config, COLLECTION_NAME);
    }
    
    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, RecordingConfig.class, COLLECTION_NAME);
    }
} 
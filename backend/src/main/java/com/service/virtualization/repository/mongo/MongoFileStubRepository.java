package com.service.virtualization.repository.mongo;

import com.service.virtualization.model.FileStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.repository.FileStubRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of FileStubRepository
 */
@Repository
public class MongoFileStubRepository implements FileStubRepository {
    
    private final MongoTemplate mongoTemplate;
    
    public MongoFileStubRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public FileStub save(FileStub fileStub) {
        // Update the updatedAt field before saving
        fileStub = new FileStub(
            fileStub.id(),
            fileStub.name(),
            fileStub.description(),
            fileStub.userId(),
            fileStub.filePath(),
            fileStub.content(),
            fileStub.contentType(),
            fileStub.files(),
            fileStub.cronExpression(),
            fileStub.status(),
            fileStub.createdAt(),
            LocalDateTime.now()
        );
        
        return mongoTemplate.save(fileStub, "fileStubs");
    }
    
    @Override
    public Optional<FileStub> findById(String id) {
        FileStub fileStub = mongoTemplate.findById(id, FileStub.class, "fileStubs");
        return Optional.ofNullable(fileStub);
    }
    
    @Override
    public List<FileStub> findAll() {
        return mongoTemplate.findAll(FileStub.class, "fileStubs");
    }
    
    @Override
    public List<FileStub> findByStatus(StubStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, FileStub.class, "fileStubs");
    }
    
    @Override
    public List<FileStub> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.find(query, FileStub.class, "fileStubs");
    }
    
    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, FileStub.class, "fileStubs");
    }
    
    @Override
    public void delete(FileStub fileStub) {
        mongoTemplate.remove(fileStub, "fileStubs");
    }
    
    @Override
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.exists(query, FileStub.class, "fileStubs");
    }
} 
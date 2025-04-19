package com.service.virtualization.repository;

import com.service.virtualization.model.FileStub;
import com.service.virtualization.model.StubStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FileStub entities
 */
@Repository
public interface FileStubRepository extends MongoRepository<FileStub, String> {
    
    /**
     * Save a file stub
     * 
     * @param fileStub the file stub to save
     * @return the saved file stub
     */
    FileStub save(FileStub fileStub);
    
    /**
     * Find a file stub by its ID
     * 
     * @param id the ID of the file stub
     * @return an Optional containing the file stub, or empty if not found
     */
    Optional<FileStub> findById(String id);
    
    /**
     * Find all file stubs
     * 
     * @return a list of all file stubs
     */
    List<FileStub> findAll();
    
    /**
     * Find file stubs by status
     * 
     * @param status the status to filter by
     * @return a list of file stubs with the specified status
     */
    List<FileStub> findByStatus(StubStatus status);
    
    /**
     * Find file stubs by user ID
     * 
     * @param userId the user ID to filter by
     * @return a list of file stubs created by the specified user
     */
    List<FileStub> findByUserId(String userId);
    
    /**
     * Delete a file stub by its ID
     * 
     * @param id the ID of the file stub to delete
     */
    void deleteById(String id);
    
    /**
     * Delete a file stub
     * 
     * @param fileStub the file stub to delete
     */
    void delete(FileStub fileStub);
    
    /**
     * Check if a file stub exists by ID
     * 
     * @param id the ID to check
     * @return true if a file stub with the given ID exists, false otherwise
     */
    boolean existsById(String id);
    
    /**
     * Find active file stubs by user ID
     * @param userId the user ID
     * @param status the status
     * @return list of file stubs
     */
    List<FileStub> findByUserIdAndStatus(String userId, boolean status);
} 
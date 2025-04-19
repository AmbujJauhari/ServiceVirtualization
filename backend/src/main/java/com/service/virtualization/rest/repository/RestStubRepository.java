package com.service.virtualization.rest.repository;

import com.service.virtualization.model.StubStatus;
import com.service.virtualization.rest.model.RestStub;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RestStub operations
 */
public interface RestStubRepository {
    
    /**
     * Save a stub
     * 
     * @param stub the stub to save
     * @return the saved stub
     */
    RestStub save(RestStub stub);
    
    /**
     * Find a stub by its ID
     * 
     * @param id the ID of the stub
     * @return an Optional containing the stub, or empty if not found
     */
    Optional<RestStub> findById(String id);
    
    /**
     * Find all stubs
     * 
     * @return a list of all stubs
     */
    List<RestStub> findAll();
    
    /**
     * Find stubs by status
     * 
     * @param status the status to filter by
     * @return a list of stubs with the specified status
     */
    List<RestStub> findByStatus(StubStatus status);
    
    /**
     * Find stubs by user ID
     * 
     * @param userId the user ID to filter by
     * @return a list of stubs created by the specified user
     */
    List<RestStub> findByUserId(String userId);
    
    /**
     * Find stubs by path
     * 
     * @param path the service path to filter by
     * @return a list of stubs for the specified path
     */
    List<RestStub> findByServicePath(String path);
    
    /**
     * Delete a stub by its ID
     * 
     * @param id the ID of the stub to delete
     */
    void deleteById(String id);
    
    /**
     * Delete a stub
     * 
     * @param stub the stub to delete
     */
    void delete(RestStub stub);
    
    /**
     * Check if a stub exists by ID
     * 
     * @param id the ID to check
     * @return true if a stub with the given ID exists, false otherwise
     */
    boolean existsById(String id);
} 
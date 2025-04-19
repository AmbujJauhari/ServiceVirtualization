package com.service.virtualization.soap;

import com.service.virtualization.model.StubStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SoapStub operations
 */
public interface SoapStubRepository {
    
    /**
     * Save a SOAP stub
     * 
     * @param stub the stub to save
     * @return the saved stub
     */
    SoapStub save(SoapStub stub);
    
    /**
     * Find a SOAP stub by its ID
     * 
     * @param id the ID of the stub
     * @return an Optional containing the stub, or empty if not found
     */
    Optional<SoapStub> findById(String id);
    
    /**
     * Find all SOAP stubs
     * 
     * @return a list of all SOAP stubs
     */
    List<SoapStub> findAll();
    
    /**
     * Find SOAP stubs by status
     * 
     * @param status the status to filter by
     * @return a list of stubs with the specified status
     */
    List<SoapStub> findByStatus(StubStatus status);
    
    /**
     * Find SOAP stubs by user ID
     * 
     * @param userId the user ID to filter by
     * @return a list of stubs created by the specified user
     */
    List<SoapStub> findByUserId(String userId);
    
    /**
     * Find SOAP stubs by service name
     * 
     * @param serviceName the service name to filter by
     * @return a list of stubs for the specified service name
     */
    List<SoapStub> findByServiceName(String serviceName);
    
    /**
     * Find SOAP stubs by operation name
     * 
     * @param operationName the operation name to filter by
     * @return a list of stubs for the specified operation name
     */
    List<SoapStub> findByOperationName(String operationName);
    
    /**
     * Delete a SOAP stub by its ID
     * 
     * @param id the ID of the stub to delete
     */
    void deleteById(String id);
    
    /**
     * Delete a SOAP stub
     * 
     * @param stub the stub to delete
     */
    void delete(SoapStub stub);
    
    /**
     * Check if a SOAP stub exists by ID
     * 
     * @param id the ID to check
     * @return true if a stub with the given ID exists, false otherwise
     */
    boolean existsById(String id);
} 
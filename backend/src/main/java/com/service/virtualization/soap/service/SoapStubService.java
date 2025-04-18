package com.service.virtualization.soap.service;

import com.service.virtualization.model.SoapStub;
import com.service.virtualization.model.StubStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for SOAP stub operations
 */
public interface SoapStubService {
    
    /**
     * Create a new SOAP stub
     * 
     * @param stub the SOAP stub to create
     * @return the created SOAP stub
     */
    SoapStub createStub(SoapStub stub);
    
    /**
     * Update an existing SOAP stub
     * 
     * @param stub the SOAP stub to update
     * @return the updated SOAP stub
     */
    SoapStub updateStub(SoapStub stub);
    
    /**
     * Delete a SOAP stub by ID
     * 
     * @param id the ID of the SOAP stub to delete
     */
    void deleteStub(String id);
    
    /**
     * Find a SOAP stub by ID
     * 
     * @param id the ID of the SOAP stub to find
     * @return an Optional containing the SOAP stub or empty if not found
     */
    Optional<SoapStub> findStubById(String id);
    
    /**
     * Find all SOAP stubs
     * 
     * @return a list of all SOAP stubs
     */
    List<SoapStub> findAllStubs();
    
    /**
     * Find SOAP stubs by user ID
     * 
     * @param userId the user ID to filter by
     * @return a list of SOAP stubs for the specified user
     */
    List<SoapStub> findStubsByUserId(String userId);
    
    /**
     * Find SOAP stubs by status
     * 
     * @param status the status to filter by
     * @return a list of SOAP stubs with the specified status
     */
    List<SoapStub> findStubsByStatus(StubStatus status);
    
    /**
     * Find SOAP stubs by service name
     * 
     * @param serviceName the service name to filter by
     * @return a list of SOAP stubs for the specified service name
     */
    List<SoapStub> findStubsByServiceName(String serviceName);
    
    /**
     * Find SOAP stubs by operation name
     * 
     * @param operationName the operation name to filter by
     * @return a list of SOAP stubs for the specified operation
     */
    List<SoapStub> findStubsByOperationName(String operationName);
    
    /**
     * Update the status of a SOAP stub
     * 
     * @param id the ID of the SOAP stub to update
     * @param status the new status
     * @return the updated SOAP stub
     */
    SoapStub updateStubStatus(String id, StubStatus status);
} 
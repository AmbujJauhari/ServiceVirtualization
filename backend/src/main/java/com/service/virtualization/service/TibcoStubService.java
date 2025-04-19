package com.service.virtualization.service;

import com.service.virtualization.model.TibcoStub;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing TIBCO EMS stubs.
 */
public interface TibcoStubService {
    /**
     * Create a new TIBCO stub.
     *
     * @param tibcoStub The TIBCO stub to create
     * @return The created TIBCO stub
     */
    TibcoStub createStub(TibcoStub tibcoStub);

    /**
     * Update an existing TIBCO stub.
     *
     * @param id The ID of the TIBCO stub to update
     * @param tibcoStub The updated TIBCO stub
     * @return The updated TIBCO stub
     */
    TibcoStub updateStub(String id, TibcoStub tibcoStub);

    /**
     * Get a TIBCO stub by its ID.
     *
     * @param id The ID of the TIBCO stub
     * @return An Optional containing the TIBCO stub if found
     */
    Optional<TibcoStub> getStubById(String id);

    /**
     * Get all TIBCO stubs.
     *
     * @return A list of all TIBCO stubs
     */
    List<TibcoStub> getAllStubs();

    /**
     * Get TIBCO stubs by status.
     *
     * @param status The status to filter by
     * @return A list of TIBCO stubs with the specified status
     */
    List<TibcoStub> getStubsByStatus(String status);

    /**
     * Get TIBCO stubs by user ID.
     *
     * @param userId The user ID to filter by
     * @return A list of TIBCO stubs created by the specified user
     */
    List<TibcoStub> getStubsByUserId(String userId);

    /**
     * Delete a TIBCO stub by its ID.
     *
     * @param id The ID of the TIBCO stub to delete
     */
    void deleteStub(String id);

    /**
     * Activate a TIBCO stub.
     *
     * @param id The ID of the TIBCO stub to activate
     * @return The activated TIBCO stub
     */
    TibcoStub activateStub(String id);

    /**
     * Deactivate a TIBCO stub.
     *
     * @param id The ID of the TIBCO stub to deactivate
     * @return The deactivated TIBCO stub
     */
    TibcoStub deactivateStub(String id);

    /**
     * Find all TIBCO stubs by status.
     *
     * @param status The status to filter by
     * @return A list of TIBCO stubs with the specified status
     */
    List<TibcoStub> findAllByStatus(String status);
} 
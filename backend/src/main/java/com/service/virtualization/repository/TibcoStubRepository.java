package com.service.virtualization.repository;

import com.service.virtualization.model.TibcoStub;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing TIBCO EMS stubs.
 */
public interface TibcoStubRepository {

    /**
     * Save a TIBCO stub.
     *
     * @param tibcoStub The TIBCO stub to save
     * @return The saved TIBCO stub
     */
    TibcoStub save(TibcoStub tibcoStub);

    /**
     * Find a TIBCO stub by its ID.
     *
     * @param id The ID of the TIBCO stub
     * @return An Optional containing the TIBCO stub if found
     */
    Optional<TibcoStub> findById(String id);

    /**
     * Find all TIBCO stubs.
     *
     * @return A list of all TIBCO stubs
     */
    List<TibcoStub> findAll();

    /**
     * Find TIBCO stubs by status.
     *
     * @param status The status to filter by
     * @return A list of TIBCO stubs with the specified status
     */
    List<TibcoStub> findByStatus(String status);

    /**
     * Find TIBCO stubs by user ID.
     *
     * @param userId The user ID to filter by
     * @return A list of TIBCO stubs created by the specified user
     */
    List<TibcoStub> findByUserId(String userId);

    /**
     * Delete a TIBCO stub by its ID.
     *
     * @param id The ID of the TIBCO stub to delete
     */
    void deleteById(String id);

    /**
     * Check if a TIBCO stub exists by its ID.
     *
     * @param id The ID of the TIBCO stub
     * @return True if the TIBCO stub exists, false otherwise
     */
    boolean existsById(String id);
} 
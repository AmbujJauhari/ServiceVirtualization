package com.service.virtualization.ibmmq.repository;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.model.StubStatus;

import java.util.List;
import java.util.Optional;

public interface IBMMQStubRepository {
    /**
     * Save a TIBCO stub.
     *
     * @param IBMMQStub The TIBCO stub to save
     * @return The saved TIBCO stub
     */
    IBMMQStub save(IBMMQStub IBMMQStub);

    /**
     * Find a TIBCO stub by its ID.
     *
     * @param id The ID of the TIBCO stub
     * @return An Optional containing the TIBCO stub if found
     */
    Optional<IBMMQStub> findById(String id);

    /**
     * Find all TIBCO stubs.
     *
     * @return A list of all TIBCO stubs
     */
    List<IBMMQStub> findAll();

    /**
     * Find TIBCO stubs by status.
     *
     * @param status The status to filter by
     * @return A list of TIBCO stubs with the specified status
     */
    List<IBMMQStub> findByStatus(StubStatus status);

    /**
     * Find TIBCO stubs by user ID.
     *
     * @param userId The user ID to filter by
     * @return A list of TIBCO stubs created by the specified user
     */
    List<IBMMQStub> findByUserId(String userId);

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
package com.service.virtualization.files;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for FileStub operations
 */
public interface FileStubService {
    
    /**
     * Create a new file stub
     * @param fileStub the file stub to create
     * @return the created file stub
     */
    FileStub create(FileStub fileStub);
    
    /**
     * Find all file stubs
     * @return list of all file stubs
     */
    List<FileStub> findAll();
    
    /**
     * Find file stubs by user ID
     * @param userId the user ID
     * @return list of file stubs
     */
    List<FileStub> findByUserId(String userId);
    
    /**
     * Find active file stubs by user ID
     * @param userId the user ID
     * @return list of active file stubs
     */
    List<FileStub> findActiveByUserId(String userId);
    
    /**
     * Find file stub by ID
     * @param id the file stub ID
     * @return the file stub if found
     */
    Optional<FileStub> findById(String id);
    
    /**
     * Update an existing file stub
     * @param id the file stub ID
     * @param fileStub the updated file stub
     * @return the updated file stub
     */
    FileStub update(String id, FileStub fileStub);
    
    /**
     * Delete a file stub by ID
     * @param id the file stub ID
     */
    void delete(String id);
    
    /**
     * Update file stub status
     * @param id the file stub ID
     * @param status the new status
     * @return the updated file stub
     */
    FileStub updateStatus(String id, boolean status);
    
    /**
     * Add a file resource to a file stub
     * @param stubId the file stub ID
     * @param file the file resource to add
     * @return the updated file stub
     */
    FileStub addFile(String stubId, FileResource file);
    
    /**
     * Remove a file resource from a file stub
     * @param stubId the file stub ID
     * @param fileId the file resource ID
     * @return the updated file stub
     */
    FileStub removeFile(String stubId, String fileId);
    
    /**
     * Get a file resource from a file stub
     * @param stubId the file stub ID
     * @param fileId the file resource ID
     * @return the file resource if found
     */
    Optional<FileResource> getFile(String stubId, String fileId);
} 
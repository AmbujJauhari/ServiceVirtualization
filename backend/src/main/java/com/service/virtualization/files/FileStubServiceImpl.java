package com.service.virtualization.files;

import com.service.virtualization.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the FileStubService interface
 */
@Service
public class FileStubServiceImpl implements FileStubService {

    private final FileStubRepository fileStubRepository;

    @Autowired
    public FileStubServiceImpl(FileStubRepository fileStubRepository) {
        this.fileStubRepository = fileStubRepository;
    }

    @Override
    public FileStub create(FileStub fileStub) {
        // Set creation and update times
        LocalDateTime now = LocalDateTime.now();
        fileStub.setCreatedAt(now);
        fileStub.setUpdatedAt(now);
        
        // Initialize files list if null
        if (fileStub.getFiles() == null) {
            fileStub.setFiles(List.of());
        }
        
        return fileStubRepository.save(fileStub);
    }

    @Override
    public List<FileStub> findAll() {
        return fileStubRepository.findAll();
    }

    @Override
    public List<FileStub> findByUserId(String userId) {
        return fileStubRepository.findByUserId(userId);
    }

    @Override
    public List<FileStub> findActiveByUserId(String userId) {
        return fileStubRepository.findByUserIdAndStatus(userId, true);
    }

    @Override
    public Optional<FileStub> findById(String id) {
        return fileStubRepository.findById(id);
    }

    @Override
    public FileStub update(String id, FileStub fileStub) {
        return fileStubRepository.findById(id)
                .map(existingFileStub -> {
                    // Update fields
                    existingFileStub.setName(fileStub.getName());
                    existingFileStub.setDescription(fileStub.getDescription());
                    existingFileStub.setStatus(fileStub.isStatus());
                    existingFileStub.setPath(fileStub.getPath());
                    existingFileStub.setUpdatedAt(LocalDateTime.now());
                    
                    // Save and return updated stub
                    return fileStubRepository.save(existingFileStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }

    @Override
    public void delete(String id) {
        fileStubRepository.deleteById(id);
    }

    @Override
    public FileStub updateStatus(String id, boolean status) {
        return fileStubRepository.findById(id)
                .map(fileStub -> {
                    fileStub.setStatus(status);
                    fileStub.setUpdatedAt(LocalDateTime.now());
                    return fileStubRepository.save(fileStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }

    @Override
    public FileStub addFile(String stubId, FileResource file) {
        return fileStubRepository.findById(stubId)
                .map(fileStub -> {
                    // Generate ID if not provided
                    if (file.getId() == null) {
                        file.setId(UUID.randomUUID().toString());
                    }
                    
                    // Add file to list
                    List<FileResource> files = fileStub.getFiles();
                    if (files == null) {
                        files = List.of(file);
                    } else {
                        files = files.stream()
                                .filter(f -> !f.getId().equals(file.getId()))
                                .collect(Collectors.toList());
                        files.add(file);
                    }
                    
                    fileStub.setFiles(files);
                    fileStub.setUpdatedAt(LocalDateTime.now());
                    
                    return fileStubRepository.save(fileStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + stubId));
    }

    @Override
    public FileStub removeFile(String stubId, String fileId) {
        return fileStubRepository.findById(stubId)
                .map(fileStub -> {
                    List<FileResource> files = fileStub.getFiles();
                    if (files != null) {
                        files = files.stream()
                                .filter(f -> !f.getId().equals(fileId))
                                .collect(Collectors.toList());
                        
                        fileStub.setFiles(files);
                        fileStub.setUpdatedAt(LocalDateTime.now());
                        return fileStubRepository.save(fileStub);
                    }
                    
                    return fileStub;
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + stubId));
    }

    @Override
    public Optional<FileResource> getFile(String stubId, String fileId) {
        return fileStubRepository.findById(stubId)
                .flatMap(fileStub -> {
                    List<FileResource> files = fileStub.getFiles();
                    if (files != null) {
                        return files.stream()
                                .filter(f -> f.getId().equals(fileId))
                                .findFirst();
                    }
                    
                    return Optional.empty();
                });
    }
} 
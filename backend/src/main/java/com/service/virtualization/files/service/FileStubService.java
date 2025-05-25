package com.service.virtualization.files.service;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.files.repository.FileStubRepository;
import com.service.virtualization.files.model.FileStub;
import com.service.virtualization.files.scheduler.FileSchedulerInitializer;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
public class FileStubService {
    private static final Logger logger = LoggerFactory.getLogger(FileStubService.class);

    private final FileStubRepository fileStubRepository;
    private final FileSchedulerInitializer schedulerInitializer;

    @Autowired
    public FileStubService(FileStubRepository fileStubRepository, @Lazy FileSchedulerInitializer schedulerInitializer) {
        this.fileStubRepository = fileStubRepository;
        this.schedulerInitializer = schedulerInitializer;
    }

    public FileStub create(FileStub fileStub) {
        LocalDateTime now = LocalDateTime.now();

        // Create new instance with updated fields
        FileStub newFileStub = new FileStub(
                fileStub.id(),
                fileStub.name(),
                fileStub.description(),
                fileStub.userId(),
                fileStub.filePath(),
                fileStub.status(),
                fileStub.cronExpression(),
                fileStub.files() != null ? fileStub.files() : List.of(),
                now,  // createdAt
                now   // updatedAt
        );

        FileStub savedStub = fileStubRepository.save(newFileStub);

        // Handle scheduling if needed
        if (schedulerInitializer != null) {
            schedulerInitializer.handleFileStubCreated(savedStub);
        }

        return savedStub;
    }

    public List<FileStub> findAll() {
        return fileStubRepository.findAll();
    }

    public List<FileStub> findByUserId(String userId) {
        return fileStubRepository.findByUserId(userId);
    }

    public List<FileStub> findActiveByUserId(String userId) {
        return fileStubRepository.findByUserIdAndStatus(userId, StubStatus.ACTIVE);
    }

    public List<FileStub> findActiveWithCronExpression() {
        return fileStubRepository.findByStatusAndCronExpressionNotNull(StubStatus.ACTIVE);
    }

    public Optional<FileStub> findById(String id) {
        return fileStubRepository.findById(id);
    }

    public FileStub update(String id, FileStub fileStub) {
        return fileStubRepository.findById(id)
                .map(existingFileStub -> {
                    // Create new instance with updated fields
                    FileStub updatedStub = new FileStub(
                            existingFileStub.id(),
                            fileStub.name(),
                            fileStub.description(),
                            existingFileStub.userId(),
                            fileStub.filePath(),
                            fileStub.status(),
                            fileStub.cronExpression(),
                            existingFileStub.files(),
                            existingFileStub.createdAt(),
                            LocalDateTime.now()
                    );

                    // Save the updated stub
                    FileStub savedStub = fileStubRepository.save(updatedStub);

                    // Handle scheduling if needed
                    if (schedulerInitializer != null) {
                        schedulerInitializer.handleFileStubUpdated(savedStub);
                    }

                    return savedStub;
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }

    public void delete(String id) {
        // Handle scheduling before deleting
        if (schedulerInitializer != null) {
            schedulerInitializer.handleFileStubDeleted(id);
        }

        fileStubRepository.deleteById(id);
    }

    public FileStub updateStatus(String id, StubStatus status) {
        return fileStubRepository.findById(id)
                .map(fileStub -> {
                    FileStub updatedStub = fileStub.withStatus(status);
                    FileStub savedStub = fileStubRepository.save(updatedStub);

                    // Handle scheduling if needed
                    if (schedulerInitializer != null) {
                        schedulerInitializer.handleFileStubStatusUpdated(savedStub);
                    }

                    return savedStub;
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }

    public FileStub updateCronExpression(String id, String cronExpression) {
        return fileStubRepository.findById(id)
                .map(fileStub -> {
                    FileStub updatedStub = fileStub.withCronExpression(cronExpression);
                    FileStub savedStub = fileStubRepository.save(updatedStub);

                    // Handle scheduling if needed
                    if (schedulerInitializer != null) {
                        schedulerInitializer.handleFileStubCronExpressionUpdated(savedStub);
                    }

                    return savedStub;
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }

    public FileStub addFile(String stubId, FileStub.FileResource file) {
        return fileStubRepository.findById(stubId)
                .map(fileStub -> {
                    // Generate ID if not provided
                    if (file.getId() == null) {
                        file.setId(UUID.randomUUID().toString());
                    }

                    // Use the record's addFile method
                    FileStub updatedStub = fileStub.addFile(file);
                    return fileStubRepository.save(updatedStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + stubId));
    }

    public FileStub removeFile(String stubId, String fileId) {
        return fileStubRepository.findById(stubId)
                .map(fileStub -> {
                    List<FileStub.FileResource> filteredFiles = fileStub.files().stream()
                            .filter(f -> !f.getId().equals(fileId))
                            .collect(Collectors.toList());

                    // Create new instance with filtered files
                    FileStub updatedStub = new FileStub(
                            fileStub.id(),
                            fileStub.name(),
                            fileStub.description(),
                            fileStub.userId(),
                            fileStub.filePath(),
                            fileStub.status(),
                            fileStub.cronExpression(),
                            filteredFiles,
                            fileStub.createdAt(),
                            LocalDateTime.now()
                    );

                    return fileStubRepository.save(updatedStub);
                })
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + stubId));
    }

    public Optional<FileStub.FileResource> getFile(String stubId, String fileId) {
        return fileStubRepository.findById(stubId)
                .flatMap(fileStub -> {
                    List<FileStub.FileResource> files = fileStub.files();
                    if (files != null) {
                        return files.stream()
                                .filter(f -> f.getId().equals(fileId))
                                .findFirst();
                    }

                    return Optional.empty();
                });
    }
} 
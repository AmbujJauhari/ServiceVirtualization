package com.service.virtualization.controller;

import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.model.FileStub;
import com.service.virtualization.model.FileResource;
import com.service.virtualization.service.FileStubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file/stubs")
public class FileStubController {

    private final FileStubService fileStubService;
    
    @Autowired
    public FileStubController(FileStubService fileStubService) {
        this.fileStubService = fileStubService;
    }
    
    @GetMapping
    public ResponseEntity<List<FileStub>> getAllFileStubs() {
        return ResponseEntity.ok(fileStubService.findAll());
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FileStub>> getFileStubsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(fileStubService.findByUserId(userId));
    }
    
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<FileStub>> getActiveFileStubsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(fileStubService.findActiveByUserId(userId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FileStub> getFileStubById(@PathVariable String id) {
        return fileStubService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }
    
    @PostMapping
    public ResponseEntity<FileStub> createFileStub(@RequestBody FileStub fileStub) {
        return new ResponseEntity<>(fileStubService.create(fileStub), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<FileStub> updateFileStub(@PathVariable String id, @RequestBody FileStub fileStub) {
        return ResponseEntity.ok(fileStubService.update(id, fileStub));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFileStub(@PathVariable String id) {
        fileStubService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<FileStub> updateFileStubStatus(@PathVariable String id, @RequestBody Map<String, Boolean> status) {
        Boolean newStatus = status.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(fileStubService.updateStatus(id, newStatus));
    }
    
    @PostMapping("/{id}/files")
    public ResponseEntity<FileStub> uploadFile(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // Get file stub
        FileStub fileStub = fileStubService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
        
        // Create storage directory if it doesn't exist
        String uploadDir = "uploads/file-stubs/" + id;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename to avoid conflicts
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
        
        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // Create file resource
        FileResource fileResource = new FileResource();
        fileResource.setId(UUID.randomUUID().toString());
        fileResource.setFilename(originalFilename);
        fileResource.setContentType(file.getContentType());
        fileResource.setSize(file.getSize());
        fileResource.setPath(filePath.toString());
        fileResource.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Add file to stub
        return ResponseEntity.ok(fileStubService.addFile(id, fileResource));
    }
    
    @DeleteMapping("/{stubId}/files/{fileId}")
    public ResponseEntity<FileStub> deleteFile(
            @PathVariable String stubId, 
            @PathVariable String fileId) {
        
        // Get file info before removing
        fileStubService.getFile(stubId, fileId).ifPresent(fileResource -> {
            try {
                // Delete file from disk
                Path filePath = Paths.get(fileResource.getPath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but continue
                System.err.println("Error deleting file: " + e.getMessage());
            }
        });
        
        // Remove file reference from stub
        return ResponseEntity.ok(fileStubService.removeFile(stubId, fileId));
    }
    
    @GetMapping("/{stubId}/files/{fileId}")
    public ResponseEntity<FileResource> getFile(
            @PathVariable String stubId,
            @PathVariable String fileId) {
        
        return fileStubService.getFile(stubId, fileId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id " + fileId));
    }
} 
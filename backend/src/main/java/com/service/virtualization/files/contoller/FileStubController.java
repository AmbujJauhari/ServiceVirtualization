package com.service.virtualization.files.contoller;

import com.service.virtualization.dto.DtoConverter;
import com.service.virtualization.exception.ResourceNotFoundException;
import com.service.virtualization.files.service.FileStubService;
import com.service.virtualization.files.model.FileStub;
import com.service.virtualization.files.dto.FileStubDTO;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file/stubs")
public class FileStubController {
    private static final Logger logger = LoggerFactory.getLogger(FileStubController.class);

    private final FileStubService fileStubService;
    
    @Autowired
    public FileStubController(FileStubService fileStubService) {
        this.fileStubService = fileStubService;
    }
    
    @GetMapping
    public ResponseEntity<List<FileStubDTO>> getAllFileStubs() {
        List<FileStubDTO> fileStubDTOs = fileStubService.findAll().stream()
                .map(DtoConverter::fromFileStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fileStubDTOs);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FileStubDTO>> getFileStubsByUserId(@PathVariable String userId) {
        List<FileStubDTO> fileStubDTOs = fileStubService.findByUserId(userId).stream()
                .map(DtoConverter::fromFileStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fileStubDTOs);
    }
    
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<FileStubDTO>> getActiveFileStubsByUserId(@PathVariable String userId) {
        List<FileStubDTO> fileStubDTOs = fileStubService.findActiveByUserId(userId).stream()
                .map(DtoConverter::fromFileStub)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fileStubDTOs);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FileStubDTO> getFileStubById(@PathVariable String id) {
        return fileStubService.findById(id)
                .map(DtoConverter::fromFileStub)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + id));
    }
    
    @PostMapping
    public ResponseEntity<FileStubDTO> createFileStub(@RequestBody FileStubDTO fileStubDTO) {
        FileStub fileStub = DtoConverter.toFileStub(fileStubDTO);
        FileStub createdFileStub = fileStubService.create(fileStub);
        return new ResponseEntity<>(DtoConverter.fromFileStub(createdFileStub), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<FileStubDTO> updateFileStub(@PathVariable String id, @RequestBody FileStubDTO fileStubDTO) {
        FileStub fileStub = DtoConverter.toFileStub(fileStubDTO);
        FileStub updatedFileStub = fileStubService.update(id, fileStub);
        return ResponseEntity.ok(DtoConverter.fromFileStub(updatedFileStub));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFileStub(@PathVariable String id) {
        fileStubService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<FileStubDTO> updateFileStubStatus(@PathVariable String id, @RequestBody Map<String, String> status) {
        try {
            StubStatus newStatus = StubStatus.valueOf(status.get("status"));
            FileStub updatedFileStub = fileStubService.updateStatus(id, newStatus);
            return ResponseEntity.ok(DtoConverter.fromFileStub(updatedFileStub));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value: {}", status.get("status"), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/files")
    public ResponseEntity<FileStubDTO> uploadFile(
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
        FileStub.FileResource fileResource = new FileStub.FileResource();
        fileResource.setId(UUID.randomUUID().toString());
        fileResource.setFilename(originalFilename);
        fileResource.setContentType(file.getContentType());
        fileResource.setSize(file.getSize());
        fileResource.setPath(filePath.toString());
        fileResource.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Add file to stub
        FileStub updatedFileStub = fileStubService.addFile(id, fileResource);
        return ResponseEntity.ok(DtoConverter.fromFileStub(updatedFileStub));
    }
    
    @DeleteMapping("/{stubId}/files/{fileId}")
    public ResponseEntity<FileStubDTO> deleteFile(
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
                logger.error("Error deleting file: {}", fileResource.getPath(), e);
            }
        });
        
        // Remove file reference from stub
        FileStub updatedFileStub = fileStubService.removeFile(stubId, fileId);
        return ResponseEntity.ok(DtoConverter.fromFileStub(updatedFileStub));
    }
    
    @GetMapping("/{stubId}/files/{fileId}")
    public ResponseEntity<FileStub.FileResource> getFile(
            @PathVariable String stubId,
            @PathVariable String fileId) {
        
        return fileStubService.getFile(stubId, fileId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id " + fileId));
    }
} 
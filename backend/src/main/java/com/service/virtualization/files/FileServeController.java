package com.service.virtualization.files;

import com.service.virtualization.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Controller for serving files from file stubs
 */
@RestController
@RequestMapping("/files")
public class FileServeController {

    private final FileStubService fileStubService;
    
    @Autowired
    public FileServeController(FileStubService fileStubService) {
        this.fileStubService = fileStubService;
    }
    
    /**
     * Serves a file by stub ID and file ID
     * 
     * @param stubId ID of the file stub
     * @param fileId ID of the file within the stub
     * @return the file content with appropriate headers
     * @throws IOException if file cannot be read
     */
    @GetMapping("/stubs/{stubId}/{fileId}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String stubId,
            @PathVariable String fileId) throws IOException {
        
        // Find the file resource
        Optional<FileResource> fileResourceOpt = fileStubService.getFile(stubId, fileId);
        if (fileResourceOpt.isEmpty()) {
            throw new ResourceNotFoundException("File not found with id " + fileId);
        }
        
        FileResource fileResource = fileResourceOpt.get();
        
        // Verify stub is active
        FileStub fileStub = fileStubService.findById(stubId)
                .orElseThrow(() -> new ResourceNotFoundException("FileStub not found with id " + stubId));
        
        if (!fileStub.isStatus()) {
            throw new ResourceNotFoundException("FileStub is not active");
        }
        
        // Load file as resource
        Path filePath = Paths.get(fileResource.getPath());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error loading file: " + e.getMessage());
        }
        
        // Determine media type
        String contentType = fileResource.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        // Set disposition header for download
        String filename = fileResource.getFilename();
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
    
    /**
     * Serves a file by path based on the stub configuration
     * 
     * @param stubPath the path configured in the stub
     * @param filename the filename to serve
     * @return the file content with appropriate headers
     * @throws IOException if file cannot be read
     */
    @GetMapping("/{*path}")
    public ResponseEntity<Resource> serveFileByPath(
            @PathVariable String path) throws IOException {
        
        // Parse path to find stub path and filename
        if (path == null || path.isEmpty()) {
            throw new ResourceNotFoundException("Invalid path");
        }
        
        // Find active stub with matching path
        Optional<FileStub> matchingStub = fileStubService.findAll().stream()
                .filter(FileStub::isStatus)
                .filter(stub -> {
                    String stubPath = stub.getPath();
                    if (stubPath == null || stubPath.isEmpty()) {
                        return false;
                    }
                    
                    // Remove leading slash for matching
                    String normalizedPath = path;
                    if (normalizedPath.startsWith("/")) {
                        normalizedPath = normalizedPath.substring(1);
                    }
                    
                    String normalizedStubPath = stubPath;
                    if (normalizedStubPath.startsWith("/")) {
                        normalizedStubPath = normalizedStubPath.substring(1);
                    }
                    
                    return normalizedPath.startsWith(normalizedStubPath);
                })
                .findFirst();
        
        if (matchingStub.isEmpty()) {
            throw new ResourceNotFoundException("No active stub found for path: " + path);
        }
        
        FileStub fileStub = matchingStub.get();
        String stubPath = fileStub.getPath();
        
        // Remove leading slash for matching
        String normalizedStubPath = stubPath;
        if (normalizedStubPath.startsWith("/")) {
            normalizedStubPath = normalizedStubPath.substring(1);
        }
        
        String normalizedPath = path;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // Extract filename from the path
        final String extractedPath = normalizedPath.substring(normalizedStubPath.length());
        final String remainingPath;
        if (extractedPath.startsWith("/")) {
            remainingPath = extractedPath.substring(1);
        } else {
            remainingPath = extractedPath;
        }
        
        // Find matching file by filename
        Optional<FileResource> matchingFile = fileStub.getFiles().stream()
                .filter(file -> file.getFilename().equals(remainingPath))
                .findFirst();
        
        if (matchingFile.isEmpty()) {
            throw new ResourceNotFoundException("File not found: " + remainingPath);
        }
        
        FileResource fileResource = matchingFile.get();
        
        // Load file as resource
        Path filePath = Paths.get(fileResource.getPath());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error loading file: " + e.getMessage());
        }
        
        // Determine media type
        String contentType = fileResource.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResource.getFilename() + "\"")
                .body(resource);
    }
} 
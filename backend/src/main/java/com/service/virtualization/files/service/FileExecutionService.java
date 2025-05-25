package com.service.virtualization.files.service;

import com.service.virtualization.files.model.FileStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for executing file stub operations
 */
@Service
public class FileExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(FileExecutionService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final FileStubService fileStubService;
    
    @Value("${virtualization.files.output.directory:./output}")
    private String outputDirectory;
    
    @Autowired
    public FileExecutionService(FileStubService fileStubService) {
        this.fileStubService = fileStubService;
    }
    
    /**
     * Execute the file stub by generating files with the content stored in the stub
     * 
     * @param stubId the file stub ID
     * @return true if execution was successful, false otherwise
     */
    public boolean executeFileStub(String stubId) {
        logger.info("Executing file stub: {}", stubId);
        
        try {
            // Get the file stub
            return fileStubService.findById(stubId)
                    .map(this::processFileStub)
                    .orElse(false);
        } catch (Exception e) {
            logger.error("Error executing file stub: {}", stubId, e);
            return false;
        }
    }
    
    /**
     * Process the file stub by generating files with the content stored in the stub
     * 
     * @param fileStub the file stub to process
     * @return true if processing was successful, false otherwise
     */
    private boolean processFileStub(FileStub fileStub) {
        logger.info("Processing file stub: {}", fileStub.id());
        
        try {
            // Create the target directory if it doesn't exist
            String targetDir = fileStub.filePath();
            if (targetDir == null || targetDir.isEmpty()) {
                targetDir = outputDirectory;
            }
            
            Path targetPath = createTargetDirectory(targetDir);
            
            // Generate all files
            List<FileStub.FileResource> files = fileStub.files();
            if (files == null || files.isEmpty()) {
                logger.warn("No files to process for file stub: {}", fileStub.id());
                return true; // Consider this a successful execution with no files
            }
            
            boolean allSuccessful = true;
            for (FileStub.FileResource file : files) {
                boolean success = generateFile(file, targetPath);
                if (!success) {
                    allSuccessful = false;
                }
            }
            
            logger.info("File stub execution completed: {} - Success: {}", fileStub.id(), allSuccessful);
            return allSuccessful;
        } catch (Exception e) {
            logger.error("Error processing file stub: {}", fileStub.id(), e);
            return false;
        }
    }
    
    /**
     * Create the target directory for file output
     * 
     * @param targetDir the target directory path
     * @return the created Path
     * @throws IOException if directory creation fails
     */
    private Path createTargetDirectory(String targetDir) throws IOException {
        Path targetPath = Paths.get(targetDir);
        
        // Create the directory if it doesn't exist
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
            logger.info("Created directory: {}", targetPath);
        }
        
        return targetPath;
    }
    
    /**
     * Generate a file with content from the FileResource
     * 
     * @param file the file resource containing metadata and content
     * @param targetPath the target directory path
     * @return true if file generation was successful, false otherwise
     */
    private boolean generateFile(FileStub.FileResource file, Path targetPath) {
        try {
            if (file.getFilename() == null || file.getFilename().isEmpty()) {
                logger.warn("Missing filename for file resource");
                return false;
            }
            
            // Get content from the file resource or retrieve it if needed
            String content = getFileContent(file);
            if (content == null) {
                logger.warn("No content available for file: {}", file.getFilename());
                return false;
            }
            
            // Generate filename with timestamp if needed
            String filename = file.getFilename();
            
            if (file.isAddTimestamp()) {
                String timestamp = LocalDateTime.now().format(DATE_FORMAT);
                String extension = "";
                int dotIndex = filename.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = filename.substring(dotIndex);
                    filename = filename.substring(0, dotIndex);
                }
                filename = filename + "_" + timestamp + extension;
            }
            
            // Create the target file
            Path filePath = targetPath.resolve(filename);
            
            // Write the content to the file
            Files.writeString(filePath, content, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("Generated file: {} with {} characters of content", filePath, content.length());
            
            return true;
        } catch (Exception e) {
            logger.error("Error generating file: {}", file.getFilename(), e);
            return false;
        }
    }
    
    /**
     * Get the content for a file resource
     * This could be extended to retrieve content from external sources if needed
     * 
     * @param file the file resource
     * @return the content of the file, or null if not available
     */
    private String getFileContent(FileStub.FileResource file) {
        // Get content directly from the file resource
        if (file.getContent() != null && !file.getContent().isEmpty()) {
            return file.getContent();
        }
        
        // If there's a path but no content, try to read from the path
        if (file.getPath() != null && !file.getPath().isEmpty()) {
            try {
                Path filePath = Paths.get(file.getPath());
                if (Files.exists(filePath)) {
                    return Files.readString(filePath);
                }
            } catch (IOException e) {
                logger.warn("Could not read file content from path: {}", file.getPath(), e);
            }
        }
        
        // No content available
        return null;
    }
} 
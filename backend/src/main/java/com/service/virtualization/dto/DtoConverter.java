package com.service.virtualization.dto;

import com.service.virtualization.files.FileEntryDTO;
import com.service.virtualization.files.dto.FileStubDTO;
import com.service.virtualization.rest.model.RestStub;
import com.service.virtualization.soap.SoapStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.files.FileEntry;
import com.service.virtualization.files.model.FileStub;
import com.service.virtualization.rest.dto.RestStubDTO;
import com.service.virtualization.soap.SoapStubDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Utility class for converting between DTOs and domain models
 */
@Component
public class DtoConverter {

    /**
     * Convert from RestStub domain model to RestStubDTO
     */
    public static RestStubDTO fromRestStub(RestStub restStub) {
        if (restStub == null) {
            return null;
        }
        
        return new RestStubDTO(
                restStub.id(),
                restStub.name(),
                restStub.description(),
                restStub.userId(),
                restStub.behindProxy(),
                restStub.protocol(),
                restStub.tags(),
                restStub.status().name(),
                restStub.createdAt(),
                restStub.updatedAt(),
                restStub.wiremockMappingId(),
                restStub.matchConditions(),
                restStub.response(),
                restStub.webhookUrl()
        );
    }

    /**
     * Convert from RestStubDTO to RestStub domain model
     */
    public static RestStub toRestStub(RestStubDTO dto) {
        if (dto == null) {
            return null;
        }
        
        StubStatus status = (dto.status() != null) 
                ? StubStatus.valueOf(dto.status()) 
                : StubStatus.INACTIVE;
                
        return new RestStub(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.userId(),
                dto.behindProxy(),
                dto.protocol(),
                dto.tags() != null ? dto.tags() : new ArrayList<>(),
                status,
                dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now(),
                dto.updatedAt() != null ? dto.updatedAt() : LocalDateTime.now(),
                dto.wiremockMappingId(),
                dto.matchConditions() != null ? dto.matchConditions() : new HashMap<>(),
                dto.response() != null ? dto.response() : new HashMap<>(),
                dto.webhookUrl()
        );
    }
    
    /**
     * Convert from SoapStub domain model to SoapStubDTO
     */
    public static SoapStubDTO fromSoapStub(SoapStub soapStub) {
        if (soapStub == null) {
            return null;
        }
        
        return new SoapStubDTO(
                soapStub.id(),
                soapStub.name(),
                soapStub.description(),
                soapStub.userId(),
                soapStub.behindProxy(),
                soapStub.protocol(),
                soapStub.tags(),
                soapStub.status().name(),
                soapStub.createdAt() != null ? soapStub.createdAt().toString() : null,
                soapStub.updatedAt() != null ? soapStub.updatedAt().toString() : null,
                soapStub.url(),
                soapStub.soapAction(),
                soapStub.webhookUrl(),
                soapStub.matchConditions(),
                soapStub.response()
        );
    }

    /**
     * Convert from SoapStubDTO to SoapStub domain model
     */
    public static SoapStub toSoapStub(SoapStubDTO dto) {
        if (dto == null) {
            return null;
        }
        
        StubStatus status = (dto.status() != null) 
                ? StubStatus.valueOf(dto.status()) 
                : StubStatus.INACTIVE;
                
        return new SoapStub(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.userId(),
                dto.behindProxy(),
                dto.protocol(),
                dto.tags() != null ? dto.tags() : new ArrayList<>(),
                status,
                dto.createdAt() != null ? LocalDateTime.parse(dto.createdAt()) : LocalDateTime.now(),
                dto.updatedAt() != null ? LocalDateTime.parse(dto.updatedAt()) : LocalDateTime.now(),
                null, // wiremockMappingId
                dto.url(),
                dto.soapAction(),
                dto.webhookUrl(),
                dto.matchConditions() != null ? dto.matchConditions() : new HashMap<>(),
                dto.response() != null ? dto.response() : new HashMap<>()
        );
    }

    /**
     * Convert a FileEntry domain model to a DTO
     */
    public static FileEntryDTO fromFileEntry(FileEntry fileEntry) {
        if (fileEntry == null) {
            return null;
        }
        
        return new FileEntryDTO(
            fileEntry.filename(),
            fileEntry.contentType(),
            fileEntry.content()
        );
    }

    /**
     * Convert a FileEntryDTO to a domain model
     */
    public static FileEntry toFileEntry(FileEntryDTO fileEntryDTO) {
        if (fileEntryDTO == null) {
            return null;
        }
        
        return new FileEntry(
            fileEntryDTO.filename(),
            fileEntryDTO.contentType(),
            fileEntryDTO.content()
        );
    }

    /**
     * Convert a FileStub domain model to a DTO
     */
    public static FileStubDTO fromFileStub(FileStub fileStub) {
        if (fileStub == null) {
            return null;
        }
        
        List<FileEntryDTO> fileEntryDTOs = fileStub.files() != null
            ? fileStub.files().stream()
                .map(file -> new FileEntryDTO(
                    file.getFilename(), 
                    file.getContentType(), 
                    null)) // Don't include file content in list view
                .collect(Collectors.toList())
            : new ArrayList<>();
            
        return new FileStubDTO(
            fileStub.id(),
            fileStub.name(),
            fileStub.description(),
            fileStub.userId(),
            fileStub.filePath(),
            null, // No direct content in FileStub record
            null, // No direct contentType in FileStub record
            fileEntryDTOs,
            fileStub.cronExpression(),
            fileStub.status() != null ? fileStub.status().name() : null,
            fileStub.createdAt(),
            fileStub.updatedAt()
        );
    }

    /**
     * Convert a FileStubDTO to a domain model
     */
    public static FileStub toFileStub(FileStubDTO fileStubDTO) {
        if (fileStubDTO == null) {
            return null;
        }
        
        List<FileStub.FileResource> fileResources = new ArrayList<>();
        if (fileStubDTO.files() != null) {
            for (FileEntryDTO entryDTO : fileStubDTO.files()) {
                FileStub.FileResource resource = new FileStub.FileResource();
                resource.setId(UUID.randomUUID().toString());
                resource.setFilename(entryDTO.filename());
                resource.setContentType(entryDTO.contentType());
                resource.setPath(null); // This will be set later when saving the file
                resource.setCreatedAt(LocalDateTime.now().toString());
                fileResources.add(resource);
            }
        }
            
        return new FileStub(
            fileStubDTO.id(),
            fileStubDTO.name(),
            fileStubDTO.description(),
            fileStubDTO.userId(),
            fileStubDTO.filePath(),
            fileStubDTO.status() != null
                ? StubStatus.valueOf(fileStubDTO.status())
                : StubStatus.INACTIVE,
            fileStubDTO.cronExpression(),
            fileResources,
            fileStubDTO.createdAt(),
            fileStubDTO.updatedAt()
        );
    }
}

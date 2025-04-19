package com.service.virtualization.dto;

import com.service.virtualization.files.FileEntryDTO;
import com.service.virtualization.files.FileStubDTO;
import com.service.virtualization.rest.model.RestStub;
import com.service.virtualization.soap.SoapStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.files.FileEntry;
import com.service.virtualization.files.FileStub;
import com.service.virtualization.rest.dto.RestStubDTO;
import com.service.virtualization.soap.SoapStubDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
                restStub.response()
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
                dto.response() != null ? dto.response() : new HashMap<>()
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
                soapStub.createdAt(),
                soapStub.updatedAt(),
                soapStub.wsdlUrl(),
                soapStub.serviceName(),
                soapStub.portName(),
                soapStub.operationName(),
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
                dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now(),
                dto.updatedAt() != null ? dto.updatedAt() : LocalDateTime.now(),
                null, // wiremockMappingId
                dto.wsdlUrl(),
                dto.serviceName(),
                dto.portName(),
                dto.operationName(),
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
        
        List<FileEntryDTO> fileEntryDTOs = fileStub.getFiles() != null
            ? fileStub.getFiles().stream()
                .map(DtoConverter::fromFileEntry)
                .collect(Collectors.toList())
            : new ArrayList<>();
            
        return new FileStubDTO(
            fileStub.id(),
            fileStub.name(),
            fileStub.description(),
            fileStub.userId(),
            fileStub.filePath(),
            fileStub.content(),
            fileStub.contentType(),
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
        
        List<FileEntry> fileEntries = fileStubDTO.files() != null
            ? fileStubDTO.files().stream()
                .map(DtoConverter::toFileEntry)
                .collect(Collectors.toList())
            : new ArrayList<>();
            
        return new FileStub(
            fileStubDTO.id(),
            fileStubDTO.name(),
            fileStubDTO.description(),
            fileStubDTO.userId(),
            fileStubDTO.filePath(),
            fileStubDTO.content(),
            fileStubDTO.contentType(),
            fileEntries,
            fileStubDTO.cronExpression(),
            fileStubDTO.status() != null
                ? StubStatus.valueOf(fileStubDTO.status())
                : StubStatus.INACTIVE,
            fileStubDTO.createdAt(),
            fileStubDTO.updatedAt()
        );
    }
}

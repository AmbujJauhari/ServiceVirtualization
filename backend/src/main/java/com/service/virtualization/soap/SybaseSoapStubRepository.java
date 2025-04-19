package com.service.virtualization.soap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.model.StubStatus;

/**
 * Sybase implementation of SoapStubRepository
 * Uses JDBC to interact with Sybase database
 */
@Repository
@Profile("sybase")
public class SybaseSoapStubRepository implements SoapStubRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    // SQL statements
    private static final String INSERT_STUB = 
            "INSERT INTO soap_stubs (id, stub_data) VALUES (?, ?)";
    
    private static final String UPDATE_STUB = 
            "UPDATE soap_stubs SET stub_data = ? WHERE id = ?";
    
    private static final String SELECT_STUB_BY_ID = 
            "SELECT * FROM soap_stubs WHERE id = ?";
    
    private static final String SELECT_ALL_STUBS = 
            "SELECT * FROM soap_stubs";
    
    private static final String SELECT_STUBS_BY_STATUS = 
            "SELECT * FROM soap_stubs WHERE JSON_VALUE(stub_data, '$.status') = ?";
    
    private static final String SELECT_STUBS_BY_USER_ID = 
            "SELECT * FROM soap_stubs WHERE JSON_VALUE(stub_data, '$.userId') = ?";
    
    private static final String SELECT_STUBS_BY_SERVICE_NAME = 
            "SELECT * FROM soap_stubs WHERE JSON_VALUE(stub_data, '$.serviceName') = ?";
    
    private static final String SELECT_STUBS_BY_OPERATION_NAME = 
            "SELECT * FROM soap_stubs WHERE JSON_VALUE(stub_data, '$.operationName') = ?";
    
    private static final String DELETE_STUB_BY_ID = 
            "DELETE FROM soap_stubs WHERE id = ?";
    
    private static final String EXISTS_STUB_BY_ID = 
            "SELECT COUNT(*) FROM soap_stubs WHERE id = ?";
    
    public SybaseSoapStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public SoapStub save(SoapStub stub) {
        try {
            String id = stub.id();
            if (id == null) {
                // Generate a new ID for insert
                id = UUID.randomUUID().toString();
                
                // Create a new stub with the generated ID
                stub = createNewStubWithId(stub, id);
            }
            
            // Convert stub to JSON
            String stubJson = serializeToJson(stub);
            
            if (existsById(id)) {
                // Update existing record
                jdbcTemplate.update(UPDATE_STUB, stubJson, id);
            } else {
                // Insert new record
                jdbcTemplate.update(INSERT_STUB, id, stubJson);
            }
            
            return stub;
        } catch (Exception e) {
            throw new RuntimeException("Error saving SOAP stub to Sybase", e);
        }
    }
    
    @Override
    public Optional<SoapStub> findById(String id) {
        try {
            SoapStub stub = jdbcTemplate.queryForObject(SELECT_STUB_BY_ID, this::mapRowToStub, id);
            return Optional.ofNullable(stub);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public List<SoapStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL_STUBS, this::mapRowToStub);
    }
    
    @Override
    public List<SoapStub> findByStatus(StubStatus status) {
        return jdbcTemplate.query(SELECT_STUBS_BY_STATUS, this::mapRowToStub, status.name());
    }
    
    @Override
    public List<SoapStub> findByUserId(String userId) {
        return jdbcTemplate.query(SELECT_STUBS_BY_USER_ID, this::mapRowToStub, userId);
    }
    
    @Override
    public List<SoapStub> findByServiceName(String serviceName) {
        return jdbcTemplate.query(SELECT_STUBS_BY_SERVICE_NAME, this::mapRowToStub, serviceName);
    }
    
    @Override
    public List<SoapStub> findByOperationName(String operationName) {
        return jdbcTemplate.query(SELECT_STUBS_BY_OPERATION_NAME, this::mapRowToStub, operationName);
    }
    
    @Override
    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update(DELETE_STUB_BY_ID, id);
    }
    
    @Override
    @Transactional
    public void delete(SoapStub stub) {
        deleteById(stub.id());
    }
    
    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_STUB_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
    
    // Helper methods
    private SoapStub mapRowToStub(ResultSet rs, int rowNum) throws SQLException {
        String stubJson = rs.getString("stub_data");
        return deserializeFromJson(stubJson);
    }
    
    private String serializeToJson(SoapStub stub) {
        try {
            return objectMapper.writeValueAsString(stub);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing SOAP stub to JSON", e);
        }
    }
    
    private SoapStub deserializeFromJson(String json) {
        try {
            return objectMapper.readValue(json, SoapStub.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON to SOAP stub", e);
        }
    }
    
    // Helper method to create a new stub with the given ID
    private SoapStub createNewStubWithId(SoapStub stub, String id) {
        return new SoapStub(
            id,
            stub.name(),
            stub.description(),
            stub.userId(),
            stub.behindProxy(),
            stub.protocol(),
            stub.tags(),
            stub.status(),
            stub.createdAt(),
            stub.updatedAt(),
            stub.wiremockMappingId(),
            stub.wsdlUrl(),
            stub.serviceName(),
            stub.portName(),
            stub.operationName(),
            stub.matchConditions(),
            stub.response()
        );
    }
} 
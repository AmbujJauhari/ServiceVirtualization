package com.service.virtualization.repository.sybase;

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
import com.service.virtualization.model.RestStub;
import com.service.virtualization.model.StubStatus;
import com.service.virtualization.repository.RestStubRepository;

/**
 * Sybase implementation of StubRepository
 * Uses JDBC to interact with Sybase database
 */
@Repository
@Profile("sybase")
public class SybaseRestStubRepository implements RestStubRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    // SQL statements
    private static final String INSERT_STUB = 
            "INSERT INTO rest_stubs (id, stub_data) VALUES (?, ?)";
    
    private static final String UPDATE_STUB = 
            "UPDATE rest_stubs SET stub_data = ? WHERE id = ?";
    
    private static final String SELECT_STUB_BY_ID = 
            "SELECT * FROM rest_stubs WHERE id = ?";
    
    private static final String SELECT_ALL_STUBS = 
            "SELECT * FROM rest_stubs";
    
    private static final String SELECT_STUBS_BY_STATUS = 
            "SELECT * FROM rest_stubs WHERE JSON_VALUE(stub_data, '$.status') = ?";
    
    private static final String SELECT_STUBS_BY_USER_ID = 
            "SELECT * FROM rest_stubs WHERE JSON_VALUE(stub_data, '$.userId') = ?";
    
    private static final String SELECT_STUBS_BY_SERVICE_PATH = 
            "SELECT * FROM rest_stubs WHERE JSON_VALUE(stub_data, '$.requestData.path') = ?";
    
    private static final String DELETE_STUB_BY_ID = 
            "DELETE FROM rest_stubs WHERE id = ?";
    
    private static final String EXISTS_STUB_BY_ID = 
            "SELECT COUNT(*) FROM rest_stubs WHERE id = ?";
    
    public SybaseRestStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public RestStub save(RestStub stub) {
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
            throw new RuntimeException("Error saving stub to Sybase", e);
        }
    }
    
    @Override
    public Optional<RestStub> findById(String id) {
        try {
            RestStub stub = jdbcTemplate.queryForObject(SELECT_STUB_BY_ID, this::mapRowToStub, id);
            return Optional.ofNullable(stub);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public List<RestStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL_STUBS, this::mapRowToStub);
    }
    
    @Override
    public List<RestStub> findByStatus(StubStatus status) {
        return jdbcTemplate.query(SELECT_STUBS_BY_STATUS, this::mapRowToStub, status.name());
    }
    
    @Override
    public List<RestStub> findByUserId(String userId) {
        return jdbcTemplate.query(SELECT_STUBS_BY_USER_ID, this::mapRowToStub, userId);
    }
    
    @Override
    public List<RestStub> findByServicePath(String path) {
        return jdbcTemplate.query(SELECT_STUBS_BY_SERVICE_PATH, this::mapRowToStub, path);
    }
    
    @Override
    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update(DELETE_STUB_BY_ID, id);
    }
    
    @Override
    @Transactional
    public void delete(RestStub stub) {
        deleteById(stub.id());
    }
    
    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_STUB_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
    
    // Helper methods
    private RestStub mapRowToStub(ResultSet rs, int rowNum) throws SQLException {
        String stubJson = rs.getString("stub_data");
        return deserializeFromJson(stubJson);
    }
    
    private String serializeToJson(RestStub stub) {
        try {
            return objectMapper.writeValueAsString(stub);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing stub to JSON", e);
        }
    }
    
    private RestStub deserializeFromJson(String json) {
        try {
            return objectMapper.readValue(json, RestStub.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON to stub", e);
        }
    }
    
    // Helper method to create a new stub with the given ID
    private RestStub createNewStubWithId(RestStub stub, String id) {
        return new RestStub(
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
            stub.matchConditions(),
            stub.response()
        );
    }
} 
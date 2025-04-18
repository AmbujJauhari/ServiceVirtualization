package com.service.virtualization.repository.sybase;

import com.service.virtualization.model.Protocol;
import com.service.virtualization.model.Recording;
import com.service.virtualization.repository.RecordingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sybase implementation of RecordingRepository
 * Uses JDBC to interact with Sybase database
 */
@Repository
@Profile("sybase")
public class SybaseRecordingRepository implements RecordingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    // SQL statements
    private static final String INSERT_RECORDING = 
            "INSERT INTO recordings (id, name, description, service_path, behind_proxy, method, " +
            "user_id, recorded_at, session_id, converted_to_stub, converted_stub_id, " +
            "request_headers, response_headers, request_body, response_body, response_status, source_ip) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_RECORDING = 
            "UPDATE recordings SET name = ?, description = ?, service_path = ?, behind_proxy = ?, " +
            "method = ?, user_id = ?, recorded_at = ?, session_id = ?, converted_to_stub = ?, " +
            "converted_stub_id = ?, request_headers = ?, response_headers = ?, request_body = ?, " +
            "response_body = ?, response_status = ?, source_ip = ? WHERE id = ?";
    
    private static final String SELECT_RECORDING_BY_ID = 
            "SELECT * FROM recordings WHERE id = ?";
    
    private static final String SELECT_ALL_RECORDINGS = 
            "SELECT * FROM recordings";
    
    private static final String SELECT_RECORDINGS_BY_SESSION_ID = 
            "SELECT * FROM recordings WHERE session_id = ?";
    
    private static final String SELECT_RECORDINGS_BY_USER_ID = 
            "SELECT * FROM recordings WHERE user_id = ?";
    
    private static final String SELECT_RECORDINGS_BY_SERVICE_PATH = 
            "SELECT * FROM recordings WHERE service_path = ?";
    
    private static final String SELECT_RECORDINGS_NOT_CONVERTED = 
            "SELECT * FROM recordings WHERE converted_to_stub = 0";
    
    private static final String DELETE_RECORDING_BY_ID = 
            "DELETE FROM recordings WHERE id = ?";
    
    private static final String EXISTS_RECORDING_BY_ID = 
            "SELECT COUNT(*) FROM recordings WHERE id = ?";
    
    public SybaseRecordingRepository(JdbcTemplate jdbcTemplate,  ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public Recording save(Recording recording) {
        try {
            String id = recording.id();
            if (id == null) {
                // Generate a new ID for insert
                id = UUID.randomUUID().toString();
                
                // Create a new recording with the generated ID
                recording = new Recording(
                    id,
                    recording.name(),
                    recording.description(),
                    recording.userId(),
                    recording.behindProxy(),
                    recording.protocol(),
                    recording.protocolData(),
                    recording.createdAt(),
                    recording.updatedAt(),
                    recording.sessionId(),
                    recording.recordedAt(),
                    recording.convertedToStub(),
                    recording.convertedStubId(),
                    recording.sourceIp(),
                    recording.requestData(),
                    recording.responseData()
                );
                
                jdbcTemplate.update(INSERT_RECORDING,
                    recording.id(),
                    recording.name(),
                    recording.description(),
                    getServicePath(recording),
                    recording.behindProxy(),
                    getMethod(recording),
                    recording.userId(),
                    Timestamp.valueOf(recording.recordedAt()),
                    recording.sessionId(),
                    recording.convertedToStub(),
                    recording.convertedStubId(),
                    serializeToJson(getRequestHeaders(recording)),
                    serializeToJson(getResponseHeaders(recording)),
                    getRequestBody(recording),
                    getResponseBody(recording),
                    getResponseStatus(recording),
                    recording.sourceIp()
                );
            } else {
                // Update existing record
                jdbcTemplate.update(UPDATE_RECORDING,
                    recording.name(),
                    recording.description(),
                    getServicePath(recording),
                    recording.behindProxy(),
                    getMethod(recording),
                    recording.userId(),
                    Timestamp.valueOf(recording.recordedAt()),
                    recording.sessionId(),
                    recording.convertedToStub(),
                    recording.convertedStubId(),
                    serializeToJson(getRequestHeaders(recording)),
                    serializeToJson(getResponseHeaders(recording)),
                    getRequestBody(recording),
                    getResponseBody(recording),
                    getResponseStatus(recording),
                    recording.sourceIp(),
                    recording.id()
                );
            }
            return recording;
        } catch (Exception e) {
            throw new RuntimeException("Error saving recording to Sybase", e);
        }
    }
    
    @Override
    public Optional<Recording> findById(String id) {
        try {
            Recording recording = jdbcTemplate.queryForObject(SELECT_RECORDING_BY_ID, this::mapRowToRecording, id);
            return Optional.ofNullable(recording);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public List<Recording> findAll() {
        return jdbcTemplate.query(SELECT_ALL_RECORDINGS, this::mapRowToRecording);
    }
    
    @Override
    public List<Recording> findBySessionId(String sessionId) {
        return jdbcTemplate.query(SELECT_RECORDINGS_BY_SESSION_ID, this::mapRowToRecording, sessionId);
    }
    
    @Override
    public List<Recording> findByUserId(String userId) {
        return jdbcTemplate.query(SELECT_RECORDINGS_BY_USER_ID, this::mapRowToRecording, userId);
    }
    
    @Override
    public List<Recording> findByServicePath(String path) {
        return jdbcTemplate.query(SELECT_RECORDINGS_BY_SERVICE_PATH, this::mapRowToRecording, path);
    }
    
    @Override
    public List<Recording> findByConvertedToStubFalse() {
        return jdbcTemplate.query(SELECT_RECORDINGS_NOT_CONVERTED, this::mapRowToRecording);
    }
    
    @Override
    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update(DELETE_RECORDING_BY_ID, id);
    }
    
    @Override
    @Transactional
    public void delete(Recording recording) {
        deleteById(recording.id());
    }
    
    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_RECORDING_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
    
    // Helper methods
    private Recording mapRowToRecording(ResultSet rs, int rowNum) throws SQLException {
        // Extract all the data from the result set
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        boolean behindProxy = rs.getBoolean("behind_proxy");
        String userId = rs.getString("user_id");
        LocalDateTime recordedAt = rs.getTimestamp("recorded_at").toLocalDateTime();
        String sessionId = rs.getString("session_id");
        boolean convertedToStub = rs.getBoolean("converted_to_stub");
        String convertedStubId = rs.getString("converted_stub_id");
        Object requestHeaders = deserializeFromJson(rs.getString("request_headers"));
        Object responseHeaders = deserializeFromJson(rs.getString("response_headers"));
        String requestBody = rs.getString("request_body");
        String responseBody = rs.getString("response_body");
        int responseStatus = rs.getInt("response_status");
        String sourceIp = rs.getString("source_ip");
        
        // Create request and response data maps
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("headers", requestHeaders);
        requestData.put("body", requestBody);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("headers", responseHeaders);
        responseData.put("body", responseBody);
        responseData.put("status", responseStatus);
        
        // Create and return a new Recording record
        return new Recording(
            id,
            name,
            description,
            userId,
            behindProxy,
            Protocol.HTTP, // Default to HTTP
            new HashMap<>(),
            LocalDateTime.now(), // For createdAt
            LocalDateTime.now(), // For updatedAt
            sessionId,
            recordedAt,
            convertedToStub,
            convertedStubId,
            sourceIp,
            requestData,
            responseData
        );
    }
    
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }
    
    private <T> T deserializeFromJson(String json) {
        try {
            return (T) objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON", e);
        }
    }
    
    // Extract legacy fields from the record's requestData/responseData maps
    private String getServicePath(Recording recording) {
        if (recording.requestData().containsKey("path")) {
            return (String) recording.requestData().get("path");
        }
        return "";
    }
    
    private String getMethod(Recording recording) {
        if (recording.requestData().containsKey("method")) {
            return (String) recording.requestData().get("method");
        }
        return "GET";
    }
    
    private Object getRequestHeaders(Recording recording) {
        if (recording.requestData().containsKey("headers")) {
            return recording.requestData().get("headers");
        }
        return new HashMap<>();
    }
    
    private Object getResponseHeaders(Recording recording) {
        if (recording.responseData().containsKey("headers")) {
            return recording.responseData().get("headers");
        }
        return new HashMap<>();
    }
    
    private String getRequestBody(Recording recording) {
        if (recording.requestData().containsKey("body")) {
            return (String) recording.requestData().get("body");
        }
        return "";
    }
    
    private String getResponseBody(Recording recording) {
        if (recording.responseData().containsKey("body")) {
            return (String) recording.responseData().get("body");
        }
        return "";
    }
    
    private int getResponseStatus(Recording recording) {
        if (recording.responseData().containsKey("status")) {
            return (int) recording.responseData().get("status");
        }
        return 200;
    }
} 
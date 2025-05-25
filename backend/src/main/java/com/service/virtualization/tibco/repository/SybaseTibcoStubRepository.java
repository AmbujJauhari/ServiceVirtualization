package com.service.virtualization.tibco.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.tibco.model.TibcoStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sybase implementation of the TibcoStubRepository interface.
 * This repository stores the entire TibcoStub object as JSON in a single column.
 */
@Repository
@Profile("sybase")
public class SybaseTibcoStubRepository implements TibcoStubRepository {
    private static final Logger logger = LoggerFactory.getLogger(SybaseTibcoStubRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // SQL statements
    private static final String TABLE_NAME = "tibco_stubs";
    private static final String INSERT_STUB = "INSERT INTO " + TABLE_NAME + " (id, stub_data) VALUES (?, ?)";
    private static final String UPDATE_STUB = "UPDATE " + TABLE_NAME + " SET stub_data = ? WHERE id = ?";
    private static final String SELECT_STUB_BY_ID = "SELECT id, stub_data FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String SELECT_ALL_STUBS = "SELECT id, stub_data FROM " + TABLE_NAME;
    private static final String SELECT_STUBS_BY_STATUS = "SELECT id, stub_data FROM " + TABLE_NAME + 
                                                       " WHERE JSON_VALUE(stub_data, '$.status') = ?";
    private static final String SELECT_STUBS_BY_USER_ID = "SELECT id, stub_data FROM " + TABLE_NAME + 
                                                        " WHERE JSON_VALUE(stub_data, '$.userId') = ?";
    private static final String DELETE_STUB_BY_ID = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String COUNT_STUBS_BY_ID = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";

    public SybaseTibcoStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public TibcoStub save(TibcoStub tibcoStub) {
        if (tibcoStub.getId() == null || tibcoStub.getId().isEmpty()) {
            tibcoStub.setId(UUID.randomUUID().toString());
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (tibcoStub.getCreatedAt() == null) {
            tibcoStub.setCreatedAt(now);
        }
        tibcoStub.setUpdatedAt(now);

        try {
            String stubJson = objectMapper.writeValueAsString(tibcoStub);
            
            if (existsById(tibcoStub.getId())) {
                jdbcTemplate.update(UPDATE_STUB, stubJson, tibcoStub.getId());
                logger.debug("Updated TIBCO stub with ID: {}", tibcoStub.getId());
            } else {
                jdbcTemplate.update(INSERT_STUB, tibcoStub.getId(), stubJson);
                logger.debug("Inserted new TIBCO stub with ID: {}", tibcoStub.getId());
            }
            
            return tibcoStub;
        } catch (JsonProcessingException e) {
            logger.error("Error serializing TIBCO stub to JSON", e);
            throw new RuntimeException("Error serializing TIBCO stub to JSON", e);
        }
    }

    @Override
    public Optional<TibcoStub> findById(String id) {
        try {
            TibcoStub tibcoStub = jdbcTemplate.queryForObject(SELECT_STUB_BY_ID, getRowMapper(), id);
            return Optional.ofNullable(tibcoStub);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("TIBCO stub not found with ID: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public List<TibcoStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL_STUBS, getRowMapper());
    }

    @Override
    public List<TibcoStub> findByStatus(String status) {
        return jdbcTemplate.query(SELECT_STUBS_BY_STATUS, getRowMapper(), status);
    }

    @Override
    public List<TibcoStub> findByUserId(String userId) {
        return jdbcTemplate.query(SELECT_STUBS_BY_USER_ID, getRowMapper(), userId);
    }

    @Override
    public void deleteById(String id) {
        int rowsAffected = jdbcTemplate.update(DELETE_STUB_BY_ID, id);
        if (rowsAffected > 0) {
            logger.debug("Deleted TIBCO stub with ID: {}", id);
        } else {
            logger.debug("No TIBCO stub found to delete with ID: {}", id);
        }
    }

    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(COUNT_STUBS_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<TibcoStub> getRowMapper() {
        return (rs, rowNum) -> {
            try {
                String stubJson = rs.getString("stub_data");
                return objectMapper.readValue(stubJson, TibcoStub.class);
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing TIBCO stub from JSON", e);
                throw new RuntimeException("Error deserializing TIBCO stub from JSON", e);
            }
        };
    }
} 
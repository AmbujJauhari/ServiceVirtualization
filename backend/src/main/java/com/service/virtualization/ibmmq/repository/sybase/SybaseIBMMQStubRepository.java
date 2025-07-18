package com.service.virtualization.ibmmq.repository.sybase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.repository.IBMMQStubRepository;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * Sybase implementation of the TibcoStubRepository interface.
 * This repository stores the entire IBMMQStub object as JSON in a single column.
 */
@Repository
@Profile("sybase")
public class SybaseIBMMQStubRepository implements IBMMQStubRepository {
    private static final Logger logger = LoggerFactory.getLogger(com.service.virtualization.tibco.repository.SybaseTibcoStubRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // SQL statements
    private static final String TABLE_NAME = "ibmmq_stubs";
    private static final String INSERT_STUB = "INSERT INTO " + TABLE_NAME + " (stub_data) VALUES (?)";
    private static final String UPDATE_STUB = "UPDATE " + TABLE_NAME + " SET stub_data = ? WHERE id = ?";
    private static final String SELECT_STUB_BY_ID = "SELECT id, stub_data FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String SELECT_ALL_STUBS = "SELECT id, stub_data FROM " + TABLE_NAME;
    private static final String SELECT_STUBS_BY_STATUS = "SELECT id, stub_data FROM " + TABLE_NAME +
            " WHERE JSON_VALUE(stub_data, '$.status') = ?";
    private static final String SELECT_STUBS_BY_USER_ID = "SELECT id, stub_data FROM " + TABLE_NAME +
            " WHERE JSON_VALUE(stub_data, '$.userId') = ?";
    private static final String DELETE_STUB_BY_ID = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String COUNT_STUBS_BY_ID = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";

    public SybaseIBMMQStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public IBMMQStub save(IBMMQStub tibcoStub) {
        try {
            if (tibcoStub.getId() == null || tibcoStub.getId().isEmpty()) {
                // Create new stub - let database generate ID
                LocalDateTime now = LocalDateTime.now();
                if (tibcoStub.getCreatedAt() == null) {
                    tibcoStub.setCreatedAt(now);
                }
                tibcoStub.setUpdatedAt(now);

                String stubJson = objectMapper.writeValueAsString(tibcoStub);

                // Insert without specifying ID - database will generate it
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            INSERT_STUB,
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, stubJson);
                    return ps;
                }, keyHolder);

                // Get the generated ID from database
                String generatedId = keyHolder.getKey().toString();
                tibcoStub.setId(generatedId);

                // Update the JSON with the generated ID
                String updatedStubJson = objectMapper.writeValueAsString(tibcoStub);
                jdbcTemplate.update(UPDATE_STUB, updatedStubJson, generatedId);

                logger.debug("Inserted new TIBCO stub with ID: {}", generatedId);
                return tibcoStub;
            } else {
                // Update existing stub
                LocalDateTime now = LocalDateTime.now();
                tibcoStub.setUpdatedAt(now);

                String stubJson = objectMapper.writeValueAsString(tibcoStub);
                jdbcTemplate.update(UPDATE_STUB, stubJson, tibcoStub.getId());
                logger.debug("Updated TIBCO stub with ID: {}", tibcoStub.getId());
                return tibcoStub;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing TIBCO stub to JSON", e);
            throw new RuntimeException("Error serializing TIBCO stub to JSON", e);
        }
    }

    @Override
    public Optional<IBMMQStub> findById(String id) {
        try {
            IBMMQStub tibcoStub = jdbcTemplate.queryForObject(SELECT_STUB_BY_ID, getRowMapper(), id);
            return Optional.ofNullable(tibcoStub);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("TIBCO stub not found with ID: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public List<IBMMQStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL_STUBS, getRowMapper());
    }

    @Override
    public List<IBMMQStub> findByStatus(StubStatus status) {
        return jdbcTemplate.query(SELECT_STUBS_BY_STATUS, getRowMapper(), status.name());
    }

    @Override
    public List<IBMMQStub> findByUserId(String userId) {
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

    private RowMapper<IBMMQStub> getRowMapper() {
        return (rs, rowNum) -> {
            try {
                String stubJson = rs.getString("stub_data");
                return objectMapper.readValue(stubJson, IBMMQStub.class);
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing TIBCO stub from JSON", e);
                throw new RuntimeException("Error deserializing TIBCO stub from JSON", e);
            }
        };
    }
}
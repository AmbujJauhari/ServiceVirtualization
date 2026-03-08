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
 * Sybase implementation of the IBMMQStubRepository interface.
 * Stores the entire IBMMQStub object as JSON in a single {@code stub_data} column.
 *
 * Expected DDL:
 * <pre>
 *   CREATE TABLE ibmmq_stubs (
 *       id       VARCHAR(36)    DEFAULT NEWID() PRIMARY KEY,
 *       stub_data NVARCHAR(MAX) NOT NULL
 *   );
 * </pre>
 */
@Repository
@Profile("sybase")
public class SybaseIBMMQStubRepository implements IBMMQStubRepository {

    private static final Logger logger = LoggerFactory.getLogger(SybaseIBMMQStubRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String TABLE_NAME = "ibmmq_stubs";
    private static final String INSERT_STUB =
            "INSERT INTO " + TABLE_NAME + " (stub_data) VALUES (?)";
    private static final String UPDATE_STUB =
            "UPDATE " + TABLE_NAME + " SET stub_data = ? WHERE id = ?";
    private static final String SELECT_BY_ID =
            "SELECT id, stub_data FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String SELECT_ALL =
            "SELECT id, stub_data FROM " + TABLE_NAME;
    private static final String SELECT_BY_STATUS =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.status') = ?";
    private static final String SELECT_BY_USER_ID =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.userId') = ?";
    private static final String DELETE_BY_ID =
            "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
    private static final String COUNT_BY_ID =
            "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";

    public SybaseIBMMQStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public IBMMQStub save(IBMMQStub stub) {
        try {
            if (stub.getId() == null || stub.getId().isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                if (stub.getCreatedAt() == null) {
                    stub.setCreatedAt(now);
                }
                stub.setUpdatedAt(now);

                String json = objectMapper.writeValueAsString(stub);

                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            INSERT_STUB, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, json);
                    return ps;
                }, keyHolder);

                String generatedId = keyHolder.getKey().toString();
                stub.setId(generatedId);

                // Embed the generated ID back into the JSON blob
                String updatedJson = objectMapper.writeValueAsString(stub);
                jdbcTemplate.update(UPDATE_STUB, updatedJson, generatedId);

                logger.debug("Inserted new IBM MQ stub with ID: {}", generatedId);
                return stub;
            } else {
                stub.setUpdatedAt(LocalDateTime.now());
                String json = objectMapper.writeValueAsString(stub);
                jdbcTemplate.update(UPDATE_STUB, json, stub.getId());
                logger.debug("Updated IBM MQ stub with ID: {}", stub.getId());
                return stub;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing IBM MQ stub to JSON", e);
            throw new RuntimeException("Error serializing IBM MQ stub to JSON", e);
        }
    }

    @Override
    public Optional<IBMMQStub> findById(String id) {
        try {
            IBMMQStub stub = jdbcTemplate.queryForObject(SELECT_BY_ID, rowMapper(), id);
            return Optional.ofNullable(stub);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("IBM MQ stub not found with ID: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public List<IBMMQStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL, rowMapper());
    }

    @Override
    public List<IBMMQStub> findByStatus(StubStatus status) {
        return jdbcTemplate.query(SELECT_BY_STATUS, rowMapper(), status.name());
    }

    @Override
    public List<IBMMQStub> findByUserId(String userId) {
        return jdbcTemplate.query(SELECT_BY_USER_ID, rowMapper(), userId);
    }

    @Override
    public void deleteById(String id) {
        int rows = jdbcTemplate.update(DELETE_BY_ID, id);
        if (rows > 0) {
            logger.debug("Deleted IBM MQ stub with ID: {}", id);
        } else {
            logger.debug("No IBM MQ stub found to delete with ID: {}", id);
        }
    }

    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(COUNT_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<IBMMQStub> rowMapper() {
        return (rs, rowNum) -> {
            try {
                return objectMapper.readValue(rs.getString("stub_data"), IBMMQStub.class);
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing IBM MQ stub from JSON", e);
                throw new RuntimeException("Error deserializing IBM MQ stub from JSON", e);
            }
        };
    }
}

package com.service.virtualization.activemq.repository.sybase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.repository.ActiveMQStubRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Sybase implementation of the ActiveMQStubRepository interface.
 * Stores the entire ActiveMQStub object as JSON in a single {@code stub_data} column,
 * preserving all fields including serverName, messageSelector, contentMatchType,
 * webhookUrl, headers, latency, etc.
 *
 * Expected DDL:
 * <pre>
 *   CREATE TABLE active_mq_stubs (
 *       id        VARCHAR(36)    DEFAULT NEWID() PRIMARY KEY,
 *       stub_data NVARCHAR(MAX)  NOT NULL
 *   );
 * </pre>
 */
@Repository
@Profile("sybase")
public class SybaseActiveMQStubRepository implements ActiveMQStubRepository {

    private static final Logger logger = LoggerFactory.getLogger(SybaseActiveMQStubRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String TABLE_NAME = "active_mq_stubs";
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
    private static final String SELECT_BY_USER_ID_AND_STATUS =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.userId') = ?"
            + "   AND JSON_VALUE(stub_data, '$.status') = ?";
    private static final String SELECT_BY_DESTINATION =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.destinationName') = ?";
    private static final String SELECT_BY_DESTINATION_TYPE_AND_PRIORITY_GT =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.destinationName') = ?"
            + "   AND JSON_VALUE(stub_data, '$.destinationType') = ?"
            + "   AND CAST(JSON_VALUE(stub_data, '$.priority') AS INT) > ?";
    private static final String SELECT_BY_DESTINATION_TYPE_AND_PRIORITY_GTE =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.destinationName') = ?"
            + "   AND JSON_VALUE(stub_data, '$.destinationType') = ?"
            + "   AND CAST(JSON_VALUE(stub_data, '$.priority') AS INT) >= ?";
    private static final String SELECT_BY_DESTINATION_AND_TYPE =
            "SELECT id, stub_data FROM " + TABLE_NAME
            + " WHERE JSON_VALUE(stub_data, '$.destinationName') = ?"
            + "   AND JSON_VALUE(stub_data, '$.destinationType') = ?";
    private static final String DELETE_BY_ID =
            "DELETE FROM " + TABLE_NAME + " WHERE id = ?";

    public SybaseActiveMQStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write operations
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ActiveMQStub save(ActiveMQStub stub) {
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

                logger.debug("Inserted new ActiveMQ stub with ID: {}", generatedId);
                return stub;
            } else {
                stub.setUpdatedAt(LocalDateTime.now());
                String json = objectMapper.writeValueAsString(stub);
                jdbcTemplate.update(UPDATE_STUB, json, stub.getId());
                logger.debug("Updated ActiveMQ stub with ID: {}", stub.getId());
                return stub;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing ActiveMQ stub to JSON", e);
            throw new RuntimeException("Error serializing ActiveMQ stub to JSON", e);
        }
    }

    @Override
    public void delete(ActiveMQStub stub) {
        jdbcTemplate.update(DELETE_BY_ID, stub.getId());
        logger.debug("Deleted ActiveMQ stub with ID: {}", stub.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read operations
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Optional<ActiveMQStub> findById(String id) {
        try {
            ActiveMQStub stub = jdbcTemplate.queryForObject(SELECT_BY_ID, rowMapper(), id);
            return Optional.ofNullable(stub);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("ActiveMQ stub not found with ID: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public List<ActiveMQStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL, rowMapper());
    }

    @Override
    public List<ActiveMQStub> findByStatus(StubStatus status) {
        return jdbcTemplate.query(SELECT_BY_STATUS, rowMapper(), status.name());
    }

    @Override
    public List<ActiveMQStub> findByUserId(String userId) {
        return jdbcTemplate.query(SELECT_BY_USER_ID, rowMapper(), userId);
    }

    @Override
    public List<ActiveMQStub> findByUserIdAndStatus(String userId, StubStatus status) {
        return jdbcTemplate.query(SELECT_BY_USER_ID_AND_STATUS, rowMapper(),
                userId, status.name());
    }

    @Override
    public List<ActiveMQStub> findByDestinationName(String destinationName) {
        return jdbcTemplate.query(SELECT_BY_DESTINATION, rowMapper(), destinationName);
    }

    @Override
    public List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThan(
            String destinationName, String destinationType, int priority) {
        return jdbcTemplate.query(SELECT_BY_DESTINATION_TYPE_AND_PRIORITY_GT, rowMapper(),
                destinationName, destinationType, priority);
    }

    @Override
    public List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThanEqual(
            String destinationName, String destinationType, int priority) {
        return jdbcTemplate.query(SELECT_BY_DESTINATION_TYPE_AND_PRIORITY_GTE, rowMapper(),
                destinationName, destinationType, priority);
    }

    /**
     * Returns the highest-priority stub for a given destination.
     *
     * Sybase ASE does not support {@code LIMIT} / {@code FETCH FIRST}.  Rather than
     * relying on dialect-specific TOP syntax we fetch all matching rows and sort
     * in Java — the result set is always small (one destination has at most a handful
     * of stubs) so this carries no meaningful overhead.
     */
    @Override
    public ActiveMQStub findFirstByDestinationNameAndDestinationTypeOrderByPriorityDesc(
            String destinationName, String destinationType) {
        List<ActiveMQStub> results = jdbcTemplate.query(
                SELECT_BY_DESTINATION_AND_TYPE, rowMapper(), destinationName, destinationType);
        return results.stream()
                .max(Comparator.comparingInt(ActiveMQStub::getPriority))
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Row mapper
    // ─────────────────────────────────────────────────────────────────────────

    private RowMapper<ActiveMQStub> rowMapper() {
        return (rs, rowNum) -> {
            try {
                return objectMapper.readValue(rs.getString("stub_data"), ActiveMQStub.class);
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing ActiveMQ stub from JSON", e);
                throw new RuntimeException("Error deserializing ActiveMQ stub from JSON", e);
            }
        };
    }
}

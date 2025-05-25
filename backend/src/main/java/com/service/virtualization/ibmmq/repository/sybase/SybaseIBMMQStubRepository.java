package com.service.virtualization.ibmmq.repository.sybase;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.ibmmq.repository.IBMMQStubRepository;
import com.service.virtualization.model.StubStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("sybase")
public class SybaseIBMMQStubRepository implements IBMMQStubRepository {
    private static final String TABLE_NAME = "ibmmq_stubs";
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<IBMMQStub> rowMapper;

    public SybaseIBMMQStubRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new IBMMQStubRowMapper();
    }

    @Override
    public List<IBMMQStub> findByUserId(String userId) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE user_id = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    @Override
    public List<IBMMQStub> findByUserIdAndStatus(String userId, StubStatus status) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE user_id = ? AND status = ?";
        return jdbcTemplate.query(sql, rowMapper, userId, status.name());
    }

    @Override
    public List<IBMMQStub> findByQueueName(String queueName) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE queue_name = ?";
        return jdbcTemplate.query(sql, rowMapper, queueName);
    }

    @Override
    public List<IBMMQStub> findByQueueManagerAndQueueName(String queueManager, String queueName) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE queue_manager = ? AND queue_name = ?";
        return jdbcTemplate.query(sql, rowMapper, queueManager, queueName);
    }

    @Override
    public List<IBMMQStub> findByQueueManagerAndQueueNameAndStatus(String queueManager, String queueName, StubStatus status) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE queue_manager = ? AND queue_name = ? AND status = ?";
        return jdbcTemplate.query(sql, rowMapper, queueManager, queueName, status.name());
    }

    @Override
    public Optional<IBMMQStub> findById(String id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        List<IBMMQStub> results = jdbcTemplate.query(sql, rowMapper, id);
        return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
    }

    @Override
    public IBMMQStub save(IBMMQStub stub) {
        if (stub.getId() == null) {
            // Insert
            String sql = "INSERT INTO " + TABLE_NAME + " (id, name, description, user_id, queue_manager, queue_name, " +
                    "selector, response_content, response_type, latency, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql,
                    stub.getId(),
                    stub.getName(),
                    stub.getDescription(),
                    stub.getUserId(),
                    stub.getQueueManager(),
                    stub.getQueueName(),
                    stub.getSelector(),
                    stub.getResponseContent(),
                    stub.getResponseType(),
                    stub.getLatency(),
                    stub.getStatus().name(),
                    stub.getCreatedAt(),
                    stub.getUpdatedAt());
        } else {
            // Update
            String sql = "UPDATE " + TABLE_NAME + " SET name = ?, description = ?, user_id = ?, queue_manager = ?, " +
                    "queue_name = ?, selector = ?, response_content = ?, response_type = ?, latency = ?, " +
                    "status = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    stub.getName(),
                    stub.getDescription(),
                    stub.getUserId(),
                    stub.getQueueManager(),
                    stub.getQueueName(),
                    stub.getSelector(),
                    stub.getResponseContent(),
                    stub.getResponseType(),
                    stub.getLatency(),
                    stub.getStatus().name(),
                    stub.getUpdatedAt(),
                    stub.getId());
        }
        return stub;
    }

    @Override
    public void delete(IBMMQStub stub) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        jdbcTemplate.update(sql, stub.getId());
    }

    @Override
    public List<IBMMQStub> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, rowMapper);
    }

    private static class IBMMQStubRowMapper implements RowMapper<IBMMQStub> {
        @Override
        public IBMMQStub mapRow(ResultSet rs, int rowNum) throws SQLException {
            IBMMQStub stub = new IBMMQStub();
            stub.setId(rs.getString("id"));
            stub.setName(rs.getString("name"));
            stub.setDescription(rs.getString("description"));
            stub.setUserId(rs.getString("user_id"));
            stub.setQueueManager(rs.getString("queue_manager"));
            stub.setQueueName(rs.getString("queue_name"));
            stub.setSelector(rs.getString("selector"));
            stub.setResponseContent(rs.getString("response_content"));
            stub.setResponseType(rs.getString("response_type"));
            stub.setLatency(rs.getInt("latency"));
            stub.setStatus(StubStatus.valueOf(rs.getString("status")));
            stub.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            stub.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return stub;
        }
    }
} 
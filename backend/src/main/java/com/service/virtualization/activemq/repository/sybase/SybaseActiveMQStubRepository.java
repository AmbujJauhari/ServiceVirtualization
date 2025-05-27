package com.service.virtualization.activemq.repository.sybase;

import com.service.virtualization.activemq.model.ActiveMQStub;
import com.service.virtualization.activemq.repository.ActiveMQStubRepository;
import com.service.virtualization.model.StubStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("sybase")
public class SybaseActiveMQStubRepository implements ActiveMQStubRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "active_mq_stubs";

    public SybaseActiveMQStubRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ActiveMQStub> rowMapper = new RowMapper<ActiveMQStub>() {
        @Override
        public ActiveMQStub mapRow(ResultSet rs, int rowNum) throws SQLException {
            ActiveMQStub stub = new ActiveMQStub();
            stub.setId(rs.getString("id"));
            stub.setName(rs.getString("name"));
            stub.setDescription(rs.getString("description"));
            stub.setUserId(rs.getString("user_id"));
            stub.setDestinationName(rs.getString("destination_name"));
            stub.setDestinationType(rs.getString("destination_type"));
            stub.setPriority(rs.getInt("priority"));
            stub.setStatus(StubStatus.valueOf(rs.getString("status")));
            stub.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            stub.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return stub;
        }
    };

    @Override
    public List<ActiveMQStub> findByStatus(StubStatus status) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE status = ?";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }
    
    @Override
    public List<ActiveMQStub> findByUserId(String userId) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE user_id = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    @Override
    public List<ActiveMQStub> findByUserIdAndStatus(String userId, StubStatus status) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE user_id = ? AND status = ?";
        return jdbcTemplate.query(sql, rowMapper, userId, status.name());
    }
    
    @Override
    public List<ActiveMQStub> findByDestinationName(String destinationName) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE destination_name = ?";
        return jdbcTemplate.query(sql, rowMapper, destinationName);
    }
    
    @Override
    public List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThan(
            String destinationName, String destinationType, int priority) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
            " WHERE destination_name = ? AND destination_type = ? AND priority > ?";
        return jdbcTemplate.query(sql, rowMapper, destinationName, destinationType, priority);
    }
    
    @Override
    public List<ActiveMQStub> findByDestinationNameAndDestinationTypeAndPriorityGreaterThanEqual(
            String destinationName, String destinationType, int priority) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
            " WHERE destination_name = ? AND destination_type = ? AND priority >= ?";
        return jdbcTemplate.query(sql, rowMapper, destinationName, destinationType, priority);
    }
            
    @Override
    public ActiveMQStub findFirstByDestinationNameAndDestinationTypeOrderByPriorityDesc(
            String destinationName, String destinationType) {
        String sql = "SELECT * FROM " + TABLE_NAME + 
            " WHERE destination_name = ? AND destination_type = ?" +
            " ORDER BY priority DESC LIMIT 1";
        List<ActiveMQStub> results = jdbcTemplate.query(sql, rowMapper, destinationName, destinationType);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public ActiveMQStub save(ActiveMQStub stub) {
        if (stub.getId() == null) {
            // Insert
            String sql = "INSERT INTO " + TABLE_NAME + 
                " (name, description, user_id, destination_name, destination_type, priority, status, created_at, updated_at)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, stub.getName());
                ps.setString(2, stub.getDescription());
                ps.setString(3, stub.getUserId());
                ps.setString(4, stub.getDestinationName());
                ps.setString(5, stub.getDestinationType());
                ps.setInt(6, stub.getPriority());
                ps.setString(7, stub.getStatus().name());
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(stub.getCreatedAt()));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(stub.getUpdatedAt()));
                return ps;
            }, keyHolder);
            stub.setId(keyHolder.getKey().toString());
        } else {
            // Update
            String sql = "UPDATE " + TABLE_NAME + 
                " SET name = ?, description = ?, user_id = ?, destination_name = ?, destination_type = ?," +
                " priority = ?, status = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                stub.getName(),
                stub.getDescription(),
                stub.getUserId(),
                stub.getDestinationName(),
                stub.getDestinationType(),
                stub.getPriority(),
                stub.getStatus().name(),
                java.sql.Timestamp.valueOf(stub.getUpdatedAt()),
                stub.getId());
        }
        return stub;
    }

    @Override
    public void delete(ActiveMQStub stub) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        jdbcTemplate.update(sql, stub.getId());
    }

    @Override
    public Optional<ActiveMQStub> findById(String id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        List<ActiveMQStub> results = jdbcTemplate.query(sql, rowMapper, id);
        return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
    }

    @Override
    public List<ActiveMQStub> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, rowMapper);
    }
} 
package com.service.virtualization.repository.sybase;

import com.service.virtualization.model.RecordingConfig;
import com.service.virtualization.repository.RecordingConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sybase implementation of RecordingConfigRepository
 */
@Repository
@Profile("sybase")
public class SybaseRecordingConfigRepository implements RecordingConfigRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(SybaseRecordingConfigRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private final RowMapper<RecordingConfig> rowMapper = (rs, rowNum) -> {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String pathPattern = rs.getString("path_pattern");
        boolean active = rs.getBoolean("active");
        String userId = rs.getString("user_id");
        boolean useHttps = rs.getBoolean("use_https");
        byte[] certificateData = rs.getBytes("certificate_data");
        String certificatePassword = rs.getString("certificate_password");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();
        
        // Get tags as comma-separated string and convert to list
        String tagsStr = rs.getString("tags");
        List<String> tags = tagsStr != null && !tagsStr.isEmpty() 
            ? Arrays.asList(tagsStr.split(","))
            : new ArrayList<>();
        
        return new RecordingConfig(
            id, name, description, pathPattern, active, userId,
            useHttps, certificateData, certificatePassword,
            createdAt, updatedAt, tags
        );
    };
    
    public SybaseRecordingConfigRepository(@Qualifier("sybaseJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        
        // Create table if it doesn't exist
        jdbcTemplate.execute(
            "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='recording_configs' AND type='U') " +
            "CREATE TABLE recording_configs (" +
            "  id VARCHAR(36) PRIMARY KEY, " +
            "  name VARCHAR(255) NOT NULL, " +
            "  description TEXT, " +
            "  path_pattern VARCHAR(255) NOT NULL, " +
            "  active BIT NOT NULL DEFAULT 0, " +
            "  user_id VARCHAR(36), " +
            "  use_https BIT NOT NULL DEFAULT 0, " +
            "  certificate_data IMAGE, " +
            "  certificate_password VARCHAR(255), " +
            "  created_at DATETIME NOT NULL, " +
            "  updated_at DATETIME NOT NULL, " +
            "  tags TEXT" +
            ")"
        );
    }
    
    @Override
    public RecordingConfig save(RecordingConfig config) {
        if (existsById(config.id())) {
            // Update existing record
            String sql = "UPDATE recording_configs SET " +
                "name = ?, description = ?, path_pattern = ?, active = ?, " +
                "user_id = ?, use_https = ?, certificate_data = ?, " +
                "certificate_password = ?, updated_at = ?, tags = ? " +
                "WHERE id = ?";
            
            // Convert tags list to comma-separated string
            String tagsStr = config.tags().stream()
                .collect(Collectors.joining(","));
            
            jdbcTemplate.update(sql,
                config.name(),
                config.description(),
                config.pathPattern(),
                config.active(),
                config.userId(),
                config.useHttps(),
                config.certificateData(),
                config.certificatePassword(),
                Timestamp.valueOf(config.updatedAt()),
                tagsStr,
                config.id()
            );
        } else {
            // Insert new record
            String sql = "INSERT INTO recording_configs " +
                "(id, name, description, path_pattern, active, user_id, " +
                "use_https, certificate_data, certificate_password, created_at, updated_at, tags) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            // Convert tags list to comma-separated string
            String tagsStr = config.tags().stream()
                .collect(Collectors.joining(","));
            
            jdbcTemplate.update(sql,
                config.id(),
                config.name(),
                config.description(),
                config.pathPattern(),
                config.active(),
                config.userId(),
                config.useHttps(),
                config.certificateData(),
                config.certificatePassword(),
                Timestamp.valueOf(config.createdAt()),
                Timestamp.valueOf(config.updatedAt()),
                tagsStr
            );
        }
        
        return config;
    }
    
    @Override
    public Optional<RecordingConfig> findById(String id) {
        String sql = "SELECT * FROM recording_configs WHERE id = ?";
        
        try {
            RecordingConfig config = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public List<RecordingConfig> findAll() {
        String sql = "SELECT * FROM recording_configs";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    @Override
    public List<RecordingConfig> findByUserId(String userId) {
        String sql = "SELECT * FROM recording_configs WHERE user_id = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    @Override
    public List<RecordingConfig> findByActiveTrue() {
        String sql = "SELECT * FROM recording_configs WHERE active = 1";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM recording_configs WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    @Override
    public void delete(RecordingConfig config) {
        deleteById(config.id());
    }
    
    @Override
    public boolean existsById(String id) {
        String sql = "SELECT COUNT(*) FROM recording_configs WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
} 
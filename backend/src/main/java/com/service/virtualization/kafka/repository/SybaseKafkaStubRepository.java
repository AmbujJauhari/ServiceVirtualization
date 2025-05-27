package com.service.virtualization.kafka.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.kafka.model.KafkaStub;
import com.service.virtualization.model.StubStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("sybase")
public class SybaseKafkaStubRepository implements KafkaStubRepository {
    private static final Logger logger = LoggerFactory.getLogger(SybaseKafkaStubRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    // SQL statements
    private static final String INSERT_STUB = "INSERT INTO kafka_stubs (stub_data) VALUES (?)";
    private static final String UPDATE_STUB = "UPDATE kafka_stubs SET stub_data = ? WHERE id = ?";
    private static final String SELECT_STUB_BY_ID = "SELECT id, stub_data FROM kafka_stubs WHERE id = ?";
    private static final String SELECT_ALL_STUBS = "SELECT id, stub_data FROM kafka_stubs";
    private static final String SELECT_STUBS_BY_USER_ID = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.userId') = ?";
    private static final String SELECT_STUBS_BY_TOPIC_AND_STATUS = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.requestTopic') = ? AND JSON_VALUE(stub_data, '$.status') = ?";
    private static final String SELECT_ACTIVE_STUBS_BY_REQUEST_TOPIC = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.requestTopic') = ? AND JSON_VALUE(stub_data, '$.status') = 'ACTIVE'";
    private static final String DELETE_STUB_BY_ID = "DELETE FROM kafka_stubs WHERE id = ?";
    private static final String EXISTS_STUB_BY_ID = "SELECT COUNT(*) FROM kafka_stubs WHERE id = ?";
    
    public SybaseKafkaStubRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public KafkaStub save(KafkaStub kafkaStub) {
        try {
            if (kafkaStub.getId() == null) {
                // Create new stub - let database generate ID
                kafkaStub.setCreatedAt(LocalDateTime.now());
                kafkaStub.setUpdatedAt(LocalDateTime.now());
                
                String stubJson = objectMapper.writeValueAsString(kafkaStub);
                
                // Insert without specifying ID - database will generate it
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO kafka_stubs (stub_data) VALUES (?)", 
                        Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, stubJson);
                    return ps;
                }, keyHolder);
                
                // Get the generated ID from database
                String generatedId = keyHolder.getKey().toString();
                kafkaStub.setId(generatedId);
                
                // Update the JSON with the generated ID
                String updatedStubJson = objectMapper.writeValueAsString(kafkaStub);
                jdbcTemplate.update(UPDATE_STUB, updatedStubJson, generatedId);
                
                return kafkaStub;
            } else {
                // Update existing stub
                kafkaStub.setUpdatedAt(LocalDateTime.now());
                String stubJson = objectMapper.writeValueAsString(kafkaStub);
                jdbcTemplate.update(UPDATE_STUB, stubJson, kafkaStub.getId());
                return kafkaStub;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing KafkaStub to JSON", e);
            throw new RuntimeException("Error serializing KafkaStub to JSON", e);
        }
    }

    @Override
    public Optional<KafkaStub> findById(String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SELECT_STUB_BY_ID, 
                (rs, rowNum) -> {
                    try {
                        String stubJson = rs.getString("stub_data");
                        return objectMapper.readValue(stubJson, KafkaStub.class);
                    } catch (JsonProcessingException e) {
                        logger.error("Error deserializing KafkaStub from JSON", e);
                        throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                    }
                }, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<KafkaStub> findAll() {
        return jdbcTemplate.query(SELECT_ALL_STUBS, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            });
    }

    @Override
    public List<KafkaStub> findAllByUserId(String userId) {
        return jdbcTemplate.query(SELECT_STUBS_BY_USER_ID, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, userId);
    }

    @Override
    public List<KafkaStub> findAllByTopicAndStatus(String topic, String status) {
        return jdbcTemplate.query(SELECT_STUBS_BY_TOPIC_AND_STATUS, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, topic, status);
    }

    @Override
    public void deleteById(String id) {
        jdbcTemplate.update(DELETE_STUB_BY_ID, id);
    }

    @Override
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_STUB_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public KafkaStub updateStatus(String id, String status) {
        Optional<KafkaStub> stubOpt = findById(id);
        if (stubOpt.isPresent()) {
            KafkaStub stub = stubOpt.get();
            stub.setStatus(status);
            stub.setUpdatedAt(LocalDateTime.now());
            return save(stub);
        } else {
            throw new RuntimeException("Kafka stub not found with id: " + id);
        }
    }

    @Override
    public List<KafkaStub> findByUserIdAndStatus(String userId, StubStatus status) {
        String sql = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.userId') = ? AND JSON_VALUE(stub_data, '$.status') = ?";
        return jdbcTemplate.query(sql, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, userId, status.name());
    }

    @Override
    public List<KafkaStub> findByUserId(String userId) {
        return findAllByUserId(userId);
    }

    @Override
    public List<KafkaStub> findByTopic(String topic) {
        String sql = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.requestTopic') = ?";
        return jdbcTemplate.query(sql, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, topic);
    }

    @Override
    public List<KafkaStub> findByTopicAndStatus(String topic, StubStatus status) {
        String sql = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.requestTopic') = ? AND JSON_VALUE(stub_data, '$.status') = ?";
        return jdbcTemplate.query(sql, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, topic, status.name());
    }

    @Override
    public void delete(KafkaStub stub) {
        deleteById(stub.getId());
    }

    @Override
    public List<KafkaStub> findActiveStubsByRequestTopic(String topic) {
        return jdbcTemplate.query(SELECT_ACTIVE_STUBS_BY_REQUEST_TOPIC, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, topic);
    }
} 
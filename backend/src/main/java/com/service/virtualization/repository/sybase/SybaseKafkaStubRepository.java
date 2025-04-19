package com.service.virtualization.repository.sybase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.virtualization.model.KafkaStub;
import com.service.virtualization.repository.KafkaStubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Repository
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
    private static final String SELECT_STUBS_BY_TOPIC_AND_STATUS = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.topic') = ? AND JSON_VALUE(stub_data, '$.status') = ?";
    private static final String SELECT_STUBS_BY_STATUS_AND_ACTIVE_FOR_PRODUCER = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.status') = ? AND JSON_VALUE(stub_data, '$.activeForProducer') = ?";
    private static final String SELECT_STUBS_BY_STATUS_AND_ACTIVE_FOR_CONSUMER = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.status') = ? AND JSON_VALUE(stub_data, '$.activeForConsumer') = ?";
    private static final String SELECT_ACTIVE_PRODUCER_STUBS_BY_TOPIC = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.topic') = ? AND JSON_VALUE(stub_data, '$.status') = 'active' AND JSON_VALUE(stub_data, '$.activeForProducer') = 'true'";
    private static final String SELECT_ACTIVE_CONSUMER_STUBS_BY_TOPIC = "SELECT id, stub_data FROM kafka_stubs WHERE JSON_VALUE(stub_data, '$.topic') = ? AND JSON_VALUE(stub_data, '$.status') = 'active' AND JSON_VALUE(stub_data, '$.activeForConsumer') = 'true'";
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
                // Create new stub
                kafkaStub.setCreatedAt(LocalDateTime.now());
                kafkaStub.setUpdatedAt(LocalDateTime.now());
                
                KeyHolder keyHolder = new GeneratedKeyHolder();
                
                String stubJson = objectMapper.writeValueAsString(kafkaStub);
                
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(INSERT_STUB, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, stubJson);
                    return ps;
                }, keyHolder);
                
                Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
                kafkaStub.setId(id);
                
                // Update the JSON with the newly assigned ID
                jdbcTemplate.update(UPDATE_STUB, objectMapper.writeValueAsString(kafkaStub), id);
                
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
    public Optional<KafkaStub> findById(Long id) {
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
    public List<KafkaStub> findActiveProducerStubsByTopic(String topic) {
        return jdbcTemplate.query(SELECT_ACTIVE_PRODUCER_STUBS_BY_TOPIC, 
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
    public List<KafkaStub> findActiveConsumerStubsByTopic(String topic) {
        return jdbcTemplate.query(SELECT_ACTIVE_CONSUMER_STUBS_BY_TOPIC, 
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
    public List<KafkaStub> findAllByStatusAndActiveForProducer(String status, Boolean activeForProducer) {
        return jdbcTemplate.query(SELECT_STUBS_BY_STATUS_AND_ACTIVE_FOR_PRODUCER, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, status, activeForProducer.toString());
    }

    @Override
    public List<KafkaStub> findAllByStatusAndActiveForConsumer(String status, Boolean activeForConsumer) {
        return jdbcTemplate.query(SELECT_STUBS_BY_STATUS_AND_ACTIVE_FOR_CONSUMER, 
            (rs, rowNum) -> {
                try {
                    String stubJson = rs.getString("stub_data");
                    return objectMapper.readValue(stubJson, KafkaStub.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing KafkaStub from JSON", e);
                    throw new RuntimeException("Error deserializing KafkaStub from JSON", e);
                }
            }, status, activeForConsumer.toString());
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update(DELETE_STUB_BY_ID, id);
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_STUB_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public KafkaStub updateStatus(Long id, String status) {
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
} 
-- Create Kafka stubs table with database-generated UUID
CREATE TABLE kafka_stubs (
    id VARCHAR(36) DEFAULT NEWID() PRIMARY KEY,
    stub_data TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_kafka_stubs_status ON kafka_stubs (JSON_VALUE(stub_data, '$.status'));
CREATE INDEX idx_kafka_stubs_user_id ON kafka_stubs (JSON_VALUE(stub_data, '$.userId'));
CREATE INDEX idx_kafka_stubs_request_topic ON kafka_stubs (JSON_VALUE(stub_data, '$.requestTopic'));
CREATE INDEX idx_kafka_stubs_response_topic ON kafka_stubs (JSON_VALUE(stub_data, '$.responseTopic')); 
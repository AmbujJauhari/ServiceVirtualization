CREATE TABLE kafka_stubs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    stub_data TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
); 
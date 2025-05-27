-- Create ActiveMQ stubs table with database-generated UUID
CREATE TABLE active_mq_stubs (
    id VARCHAR(36) DEFAULT NEWID() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id VARCHAR(100) NOT NULL,
    destination_name VARCHAR(255) NOT NULL,
    destination_type VARCHAR(50) NOT NULL,
    priority INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create IBM MQ stubs table with database-generated UUID
CREATE TABLE ibmmq_stubs (
    id VARCHAR(36) DEFAULT NEWID() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id VARCHAR(100) NOT NULL,
    queue_manager VARCHAR(255) NOT NULL,
    queue_name VARCHAR(255) NOT NULL,
    selector VARCHAR(1000),
    response_content TEXT,
    response_type VARCHAR(50),
    latency INT DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_active_mq_stubs_status ON active_mq_stubs (status);
CREATE INDEX idx_active_mq_stubs_user_id ON active_mq_stubs (user_id);
CREATE INDEX idx_active_mq_stubs_destination ON active_mq_stubs (destination_name, destination_type);

CREATE INDEX idx_ibmmq_stubs_status ON ibmmq_stubs (status);
CREATE INDEX idx_ibmmq_stubs_user_id ON ibmmq_stubs (user_id);
CREATE INDEX idx_ibmmq_stubs_queue ON ibmmq_stubs (queue_manager, queue_name); 
-- Add missing fields to IBM MQ stubs table to match ActiveMQ feature parity

-- Add content matching configuration fields
ALTER TABLE ibmmq_stubs ADD content_match_type VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE ibmmq_stubs ADD content_pattern TEXT;
ALTER TABLE ibmmq_stubs ADD case_sensitive BIT DEFAULT 0;

-- Add priority field for stub matching
ALTER TABLE ibmmq_stubs ADD priority INT DEFAULT 0;

-- Add response configuration fields
ALTER TABLE ibmmq_stubs ADD response_destination VARCHAR(255);
ALTER TABLE ibmmq_stubs ADD response_destination_type VARCHAR(50) DEFAULT 'queue';
ALTER TABLE ibmmq_stubs ADD webhook_url VARCHAR(1000);

-- Create additional indexes for efficient querying
CREATE INDEX idx_ibmmq_stubs_priority ON ibmmq_stubs (priority);
CREATE INDEX idx_ibmmq_stubs_content_match ON ibmmq_stubs (content_match_type);
CREATE INDEX idx_ibmmq_stubs_queue_priority ON ibmmq_stubs (queue_manager, queue_name, priority); 
-- Create TIBCO stubs table with database-generated UUID
CREATE TABLE tibco_stubs (
    id VARCHAR(36) DEFAULT NEWID() PRIMARY KEY,
    stub_data NVARCHAR(MAX) NOT NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_tibco_stubs_status ON tibco_stubs (JSON_VALUE(stub_data, '$.status'));
CREATE INDEX idx_tibco_stubs_user_id ON tibco_stubs (JSON_VALUE(stub_data, '$.userId')); 
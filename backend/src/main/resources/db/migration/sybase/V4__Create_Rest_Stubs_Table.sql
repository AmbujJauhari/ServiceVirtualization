-- Create REST stubs table with database-generated UUID
CREATE TABLE rest_stubs (
    id VARCHAR(36) DEFAULT NEWID() PRIMARY KEY,
    stub_data NVARCHAR(MAX) NOT NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_rest_stubs_status ON rest_stubs (JSON_VALUE(stub_data, '$.status'));
CREATE INDEX idx_rest_stubs_user_id ON rest_stubs (JSON_VALUE(stub_data, '$.userId'));
CREATE INDEX idx_rest_stubs_service_path ON rest_stubs (JSON_VALUE(stub_data, '$.requestData.filePath')); 
-- Create SOAP stubs table for Sybase
CREATE TABLE soap_stubs (
    id VARCHAR(36) PRIMARY KEY,
    stub_data NVARCHAR(MAX) NOT NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_soap_stubs_status ON soap_stubs (JSON_VALUE(stub_data, '$.status'));
CREATE INDEX idx_soap_stubs_service_name ON soap_stubs (JSON_VALUE(stub_data, '$.serviceName'));
CREATE INDEX idx_soap_stubs_operation_name ON soap_stubs (JSON_VALUE(stub_data, '$.operationName'));
CREATE INDEX idx_soap_stubs_user_id ON soap_stubs (JSON_VALUE(stub_data, '$.userId')); 
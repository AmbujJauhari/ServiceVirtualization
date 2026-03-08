-- ActiveMQ stubs — JSON blob storage (mirrors REST, SOAP, Kafka, Tibco approach)
-- All fields of ActiveMQStub (serverName, messageSelector, contentMatchType,
-- webhookUrl, headers, latency, etc.) are preserved in the stub_data column.
CREATE TABLE active_mq_stubs (
    id        VARCHAR(36)   DEFAULT NEWID() PRIMARY KEY,
    stub_data NVARCHAR(MAX) NOT NULL
);

CREATE INDEX idx_active_mq_stubs_status
    ON active_mq_stubs (JSON_VALUE(stub_data, '$.status'));

CREATE INDEX idx_active_mq_stubs_user_id
    ON active_mq_stubs (JSON_VALUE(stub_data, '$.userId'));

CREATE INDEX idx_active_mq_stubs_destination
    ON active_mq_stubs (
        JSON_VALUE(stub_data, '$.destinationName'),
        JSON_VALUE(stub_data, '$.destinationType')
    );


-- IBM MQ stubs — JSON blob storage
-- All fields of IBMMQStub are preserved in the stub_data column.
CREATE TABLE ibmmq_stubs (
    id        VARCHAR(36)   DEFAULT NEWID() PRIMARY KEY,
    stub_data NVARCHAR(MAX) NOT NULL
);

CREATE INDEX idx_ibmmq_stubs_status
    ON ibmmq_stubs (JSON_VALUE(stub_data, '$.status'));

CREATE INDEX idx_ibmmq_stubs_user_id
    ON ibmmq_stubs (JSON_VALUE(stub_data, '$.userId'));

CREATE INDEX idx_ibmmq_stubs_queue
    ON ibmmq_stubs (
        JSON_VALUE(stub_data, '$.queueManager'),
        JSON_VALUE(stub_data, '$.queueName')
    );

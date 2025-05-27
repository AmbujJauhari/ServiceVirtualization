-- Update existing tables to use database-generated UUIDs for new records
-- This migration ensures that new records will have auto-generated IDs

-- Update Kafka stubs table to use auto-generated UUIDs
ALTER TABLE kafka_stubs ADD CONSTRAINT DF_kafka_stubs_id DEFAULT NEWID() FOR id;

-- Update SOAP stubs table to use auto-generated UUIDs  
ALTER TABLE soap_stubs ADD CONSTRAINT DF_soap_stubs_id DEFAULT NEWID() FOR id;

-- Update REST stubs table to use auto-generated UUIDs
ALTER TABLE rest_stubs ADD CONSTRAINT DF_rest_stubs_id DEFAULT NEWID() FOR id;

-- Update TIBCO stubs table to use auto-generated UUIDs
ALTER TABLE tibco_stubs ADD CONSTRAINT DF_tibco_stubs_id DEFAULT NEWID() FOR id;

-- Update ActiveMQ stubs table to use auto-generated UUIDs
ALTER TABLE active_mq_stubs ADD CONSTRAINT DF_active_mq_stubs_id DEFAULT NEWID() FOR id;

-- Update IBM MQ stubs table to use auto-generated UUIDs
ALTER TABLE ibmmq_stubs ADD CONSTRAINT DF_ibmmq_stubs_id DEFAULT NEWID() FOR id;

-- Note: Existing records will keep their current IDs
-- Only new records (where ID is not explicitly provided) will get auto-generated UUIDs 
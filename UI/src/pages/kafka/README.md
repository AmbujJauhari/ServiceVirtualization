# Kafka Publisher - Enhanced Features with RBAC Schema Registry

## Overview
The Kafka Publisher now supports custom topic creation and **RBAC-protected Schema Registry integration**, providing flexible messaging capabilities with enterprise-grade schema validation.

## New Features

### 1. Smart Topic Selection
- **Single input field**: Type topic name or select from suggestions
- **Auto-suggestions**: Shows existing topics as you type
- **Auto-creation**: Topics are automatically created when publishing if they don't exist
- **Seamless UX**: No need to choose between "existing" vs "new" - just type the topic name

### 2. RBAC Schema Registry Support
- **Schema discovery**: Browse available schemas from registry
- **Dropdown selection**: Choose schema subject and version from lists
- **Real-time validation**: Messages validated against selected schema before publishing
- **RBAC authentication**: Backend handles authentication to protected registries
- **Error feedback**: Clear validation error messages displayed in UI

## Usage

### Smart Topic Selection
1. Start typing in the "Topic Name" field
2. **For existing topics**: Select from auto-suggestions dropdown
3. **For new topics**: Simply type the desired topic name
4. Topic will be created automatically if it doesn't exist

### Using Schema Registry
1. Check "Use Schema Registry" checkbox
2. **Option A**: Select schema subject from dropdown and choose version
3. **Option B**: Enter direct schema ID if known
4. Message will be validated against selected schema before publishing
5. Validation errors will be displayed if message doesn't conform to schema

### Content Types
- **JSON**: Standard JSON messages with optional JSON schema validation
- **XML**: XML formatted messages  
- **Plain Text**: Simple text messages
- **Avro**: Binary Avro messages (automatically enables Schema Registry with Avro schema validation)

## Backend Integration

### RBAC Authentication
The backend handles all authentication to Schema Registry:
- **Basic Auth**: Username/password authentication
- **API Key**: API key-based authentication  
- **Bearer Token**: OAuth/JWT token authentication
- **Automatic retry**: Built-in retry logic for authentication failures

### Schema Validation
- **Pre-publish validation**: Messages validated before sending to Kafka
- **Format-specific validation**: Avro vs JSON schema validation
- **Detailed error messages**: Clear feedback on validation failures
- **Schema caching**: Frequently used schemas cached for performance

### Configuration
Set environment variables for RBAC:
```bash
# For RBAC-protected Schema Registry (Confluent Cloud)
SCHEMA_REGISTRY_URL=https://your-schema-registry.com
SCHEMA_REGISTRY_AUTH_TYPE=basic  # basic, apikey, or bearer
SCHEMA_REGISTRY_USERNAME=your-username
SCHEMA_REGISTRY_PASSWORD=your-password
# OR
SCHEMA_REGISTRY_API_KEY=your-api-key
```

**For Non-RBAC Schema Registries (Local/Development):**
```bash
# For open Schema Registry (no authentication required)
SCHEMA_REGISTRY_URL=http://localhost:8081
SCHEMA_REGISTRY_AUTH_TYPE=none  # No authentication
# Username, password, and API key can be omitted
```

## API Endpoints

### Schema Discovery
- `GET /api/kafka/schemas` - Get available schemas
- `GET /api/kafka/schemas/{subject}/versions` - Get schema versions
- `POST /api/kafka/validate-schema` - Validate message against schema

### Enhanced Publishing
- `POST /api/kafka/publish` - Publish with automatic schema validation

## Docker Configuration

### Access URLs
- **Kafka UI**: http://localhost:8082
- **Kafka Broker**: localhost:9092 (external), kafka:29092 (internal)
- **Zookeeper**: localhost:2181 (external), zookeeper:2181 (internal)
- **Schema Registry**: http://localhost:8081 (if running locally)

### Environment Variables for RBAC
Add to your docker-compose.yml or environment:
```yaml
# For RBAC-protected Schema Registry
environment:
  SCHEMA_REGISTRY_URL: https://your-confluent-cloud-sr.com
  SCHEMA_REGISTRY_AUTH_TYPE: basic
  SCHEMA_REGISTRY_USERNAME: your-sr-api-key
  SCHEMA_REGISTRY_PASSWORD: your-sr-api-secret
```

**For Local Development (Non-RBAC):**
```yaml
# Add Schema Registry service to your docker-compose.yml
services:
  schema-registry:
    image: confluentinc/cp-schema-registry:7.3.2
    container_name: sv-schema-registry
    ports:
      - "8081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:29092
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081

# Your application environment
environment:
  SCHEMA_REGISTRY_URL: http://localhost:8081
  SCHEMA_REGISTRY_AUTH_TYPE: none  # No authentication for local registry
```

## Examples

### Avro Message with RBAC Schema Registry
```
Topic: user-events
Content Type: Avro
Schema Registry: ✓ Enabled
Schema Subject: user-events-value (selected from dropdown)
Schema Version: 2 (selected from dropdown)
Message: {"userId": 123, "event": "signup", "timestamp": 1640995200}
Result: ✓ Validated against Avro schema before publishing
```

### JSON Message with Schema Validation
```
Topic: api-events
Content Type: JSON
Schema Registry: ✓ Enabled  
Schema Subject: api-events-schema
Schema Version: latest
Message: {"endpoint": "/api/users", "method": "POST", "status": 201}
Result: ✓ Validated against JSON schema before publishing
```

### Custom Topic with Schema
```
Topic: my-new-topic (typed into input field)
Content Type: JSON
Schema ID: 456 (direct schema ID)
Message: {"customField": "value"}
Result: ✓ Topic created + Message validated + Published
```

## Error Handling

### Schema Validation Errors
- **Format errors**: "Message must be valid JSON format"
- **Schema mismatch**: "Field 'userId' is required but missing"
- **Type errors**: "Field 'timestamp' must be integer, got string"
- **Registry errors**: "Schema not found in registry"

### Authentication Errors
- **RBAC failures**: Handled transparently with proper error logging
- **Network issues**: Automatic retry with exponential backoff
- **Permission denied**: Clear error messages for insufficient permissions

## Troubleshooting

### Schema Registry Issues
- **RBAC Authentication**: Check environment variables for auth credentials
- **Network connectivity**: Verify Schema Registry URL is accessible
- **Permissions**: Ensure user has read access to schemas
- **Schema not found**: Verify schema exists and user has access

**For Non-RBAC Registries:**
- **Connection refused**: Ensure Schema Registry is running on specified port
- **Empty schema list**: Check if any schemas are registered in the registry
- **Network issues**: Verify URL is accessible: `curl http://localhost:8081/subjects`
- **Docker setup**: Ensure Schema Registry container is connected to same network as your app

### Validation Issues
- **Message format**: Ensure message matches expected content type
- **Schema compatibility**: Check if message conforms to schema structure
- **Version conflicts**: Verify correct schema version is selected

### Connection Issues
- **Schema Registry**: Check logs: `docker logs your-app-container`
- **Kafka issues**: Restart containers: `docker-compose restart`
- **Authentication**: Verify RBAC credentials in application logs 
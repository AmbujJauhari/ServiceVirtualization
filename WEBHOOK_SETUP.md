# Webhook Support for REST Stubs

This implementation adds webhook support to the Service Virtualization platform, allowing REST stubs to call external webhook URLs and return dynamic responses.

## Architecture Overview

- **Main Service**: Runs on port 8080 (unchanged)
- **Embedded WireMock**: Runs on port 8081 with custom webhook transformer
- **Direct Client Interaction**: Clients call WireMock directly on port 8081
- **Dynamic Responses**: WireMock calls webhook URLs and returns the responses

## Key Components

### 1. WebhookResponseTransformer
- Custom WireMock response transformer
- Handles webhook calls when stubs are configured with webhook URLs
- Forwards original request data to webhook endpoints
- Returns webhook response as the stub response

### 2. RestWebhookService
- Service for making HTTP calls to webhook URLs
- Handles request forwarding and response processing
- Includes error handling and logging

### 3. Enhanced RestStub Model
- Added `webhookUrl` field to support webhook configuration
- Backward compatible - existing stubs continue to work
- New method `hasWebhook()` to check if webhook is configured

### 4. TestWebhookController
- Test endpoint at `/test-webhook/**` for testing webhook functionality
- Logs all received requests
- Provides different response types based on path
- Includes request history and management endpoints

## How It Works

1. **Stub Creation**: Create a REST stub with a webhook URL
2. **WireMock Registration**: Stub is registered with embedded WireMock including webhook transformer
3. **Client Request**: Client sends request directly to WireMock (port 8081)
4. **Webhook Call**: WireMock transformer calls the configured webhook URL with original request data
5. **Dynamic Response**: Webhook response is returned to the client

## Configuration

### Application Properties
```yaml
wiremock:
  server:
    host: localhost
    port: 8081  # Embedded WireMock port
```

### REST Stub with Webhook
```json
{
  "name": "Dynamic User Service",
  "webhookUrl": "http://localhost:8080/test-webhook/users",
  "matchConditions": {
    "method": "GET",
    "url": "/api/users",
    "urlMatchType": "exact"
  },
  "response": {
    "status": 200,
    "headers": [
      {"name": "Content-Type", "value": "application/json"}
    ],
    "body": "{\"default\": \"response\"}"
  }
}
```

## Testing the Webhook Functionality

### 1. Start the Application
```bash
cd backend
mvn spring-boot:run
```

### 2. Create a Stub with Webhook
```bash
curl -X POST http://localhost:8080/api/rest/stubs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Webhook Stub",
    "webhookUrl": "http://localhost:8080/test-webhook/dynamic",
    "userId": "test-user",
    "matchConditions": {
      "method": "GET",
      "url": "/api/test",
      "urlMatchType": "exact"
    },
    "response": {
      "status": 200,
      "body": "Default response"
    }
  }'
```

### 3. Test the Webhook
```bash
# Make request to WireMock directly
curl http://localhost:8081/api/test

# Check webhook history
curl http://localhost:8080/test-webhook/history
```

## Webhook Request Format

When a request hits a stub with webhook configured, the webhook URL receives:

### Headers
- `X-Original-{HeaderName}`: Original request headers
- `X-Stub-ID`: The ID of the stub that triggered the webhook
- `X-Original-Method`: HTTP method of the original request
- `X-Original-URL`: URL of the original request
- `X-Original-Query`: Query parameters (if any)

### Body
- Original request body is forwarded as-is

## Response Handling

- **Success (2xx)**: Webhook response body is returned to client
- **Error/Timeout**: Original stub response is returned as fallback
- **Additional Headers**: `X-Webhook-Response: true` added to indicate dynamic response

## Backward Compatibility

- Existing stubs without webhook URLs continue to work normally
- No breaking changes to existing API contracts
- Optional webhook field in all DTOs and models

## Test Webhook Endpoints

The built-in test webhook controller provides several test endpoints:

- `/test-webhook/json` - Returns JSON response
- `/test-webhook/text` - Returns plain text
- `/test-webhook/error` - Returns error response
- `/test-webhook/history` - View request history
- `/test-webhook/**` - Catch-all for any path

## Error Handling

- Webhook call failures gracefully fall back to original stub response
- Comprehensive logging for debugging
- Error headers added to responses for troubleshooting
- Timeout handling for slow webhook endpoints

## Security Considerations

- Webhook URLs should be validated and whitelisted in production
- Consider authentication mechanisms for webhook endpoints
- Monitor webhook call patterns for potential abuse
- Implement rate limiting if needed

## Performance Notes

- Webhook calls are synchronous and may impact response times
- Consider implementing async webhooks for heavy processing
- Monitor webhook endpoint performance
- Implement caching strategies if appropriate

## Troubleshooting

### Common Issues

1. **Webhook not called**: Check if stub has webhook URL configured
2. **Connection errors**: Verify webhook URL is accessible
3. **Transformer not working**: Check WireMock logs for transformer registration
4. **Response not returned**: Verify webhook returns 2xx status code

### Debugging

- Check application logs for webhook call details
- Use `/test-webhook/history` to see received requests
- Monitor WireMock logs for transformer activity
- Verify stub registration with `/api/rest/stubs` 
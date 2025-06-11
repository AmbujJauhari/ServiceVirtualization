# Kafka Disabled Profile Implementation

This document describes the implementation of the `kafka-disabled` profile feature, which allows selective disabling of the Kafka protocol in Service Virtualization deployments.

## Overview

The implementation follows an **inverted approach** where:
- **Default**: All protocols are enabled by default
- **Selective Disabling**: Use profiles like `kafka-disabled` to turn off specific protocols
- **Backward Compatible**: Existing deployments remain unchanged
- **Developer Friendly**: Full functionality available by default
- **Operations Friendly**: Simple disable flags for production

## Backend Implementation

### 1. Profile-Based Bean Loading

All Kafka-related Spring beans are annotated with `@Profile("!kafka-disabled")`:

```java
@Configuration
@Profile("!kafka-disabled")
public class KafkaConfig {
    // All Kafka configuration beans
}

@RestController
@Profile("!kafka-disabled")
public class KafkaStubController {
    // Kafka API endpoints
}

@Service
@Profile("!kafka-disabled")
public class KafkaMessageService {
    // Kafka business logic
}
```

### 2. Affected Components

The following components are disabled when `kafka-disabled` profile is active:

**Configuration:**
- `KafkaConfig` - All Kafka beans (producers, consumers, admin, templates)

**Controllers:**
- `KafkaStubController` - Stub management APIs
- `KafkaMessageController` - Message publishing APIs
- `KafkaWebhookTestController` - Webhook testing APIs

**Services:**
- `KafkaStubService` - Stub business logic
- `KafkaMessageService` - Message handling
- `KafkaTopicService` - Topic management
- `KafkaCallbackService` - Webhook callbacks
- `SchemaRegistryService` - Schema validation
- `KafkaStubListenerService` - Message listeners

### 3. Protocol Status API

New endpoint: `GET /api/health/protocols`

```json
{
  "protocols": [
    {
      "name": "Kafka",
      "enabled": false,
      "reason": "Disabled by kafka-disabled profile"
    },
    {
      "name": "ActiveMQ", 
      "enabled": true,
      "reason": "Enabled by default"
    }
  ],
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Frontend Implementation

### 1. Protocol Status API Integration

```typescript
// New API endpoints in healthApi.ts
export interface ProtocolStatus {
  name: string;
  enabled: boolean;
  reason: string;
}

export const { useGetProtocolStatusQuery } = healthApi;
```

### 2. Dashboard Updates

**Main Dashboard (`Dashboard.tsx`):**
- Shows protocol status badges (Enabled/Disabled)
- Disables buttons for unavailable protocols
- Displays reason for disabled protocols

**Kafka Dashboard (`KafkaDashboard.tsx`):**
- Checks protocol status on load
- Shows disabled state with explanation
- Prevents access to Kafka functionality

### 3. Visual Indicators

- **Enabled**: Green badge, active buttons
- **Disabled**: Red badge, grayed out cards, disabled buttons
- **Loading**: Spinner while checking status
- **Error**: Warning message if status check fails

## Usage

### 1. Enable Kafka (Default)

No configuration needed. Kafka is enabled by default.

```properties
# application.properties - nothing needed
```

### 2. Disable Kafka

Add the profile to your configuration:

```properties
# application.properties
spring.profiles.active=kafka-disabled
```

Or via command line:
```bash
java -jar app.jar --spring.profiles.active=kafka-disabled
```

Or via environment variable:
```bash
SPRING_PROFILES_ACTIVE=kafka-disabled java -jar app.jar
```

### 3. Multiple Profiles

Combine with other profiles:
```properties
spring.profiles.active=production,kafka-disabled
```

## Benefits

### 1. Resource Optimization
- No Kafka beans loaded when disabled
- No Kafka connections attempted
- Reduced memory footprint
- Faster startup time

### 2. Deployment Flexibility
- Lightweight deployments for specific use cases
- Environment-specific protocol selection
- Easy testing of protocol subsets

### 3. Operational Safety
- Prevents accidental Kafka operations when not needed
- Clear visual indicators in UI
- Graceful degradation

### 4. Development Experience
- Full functionality by default
- No configuration needed for development
- Easy to test disabled states

## Testing

### 1. Test with Disabled Profile

```bash
# Start with Kafka disabled
cd backend
java -jar target/service-virtualization.jar --spring.profiles.active=kafka-disabled
```

### 2. Verify Backend Behavior

```bash
# Should return 404 (no beans loaded)
curl http://localhost:8080/api/kafka/stubs

# Should show Kafka as disabled
curl http://localhost:8080/api/health/protocols
```

### 3. Verify Frontend Behavior

1. Navigate to main dashboard
2. Kafka card should show "Disabled" badge
3. Button should be grayed out and say "Unavailable"
4. Navigate to `/kafka` should show disabled state message

## Future Extensions

This implementation serves as a template for other protocols:

1. **IBM MQ**: `ibmmq-disabled` profile
2. **TIBCO**: `tibco-disabled` profile  
3. **ActiveMQ**: `activemq-disabled` profile

The same pattern can be applied:
1. Add `@Profile("!protocol-disabled")` to all beans
2. Update protocol status endpoint
3. Add UI handling for disabled state

## Configuration Examples

### Development (All Enabled)
```properties
# No configuration needed - everything enabled by default
```

### Production - Messaging Only
```properties
spring.profiles.active=rest-disabled,soap-disabled
```

### Testing - Kafka Only
```properties
spring.profiles.active=activemq-disabled,ibmmq-disabled,tibco-disabled
```

### Lightweight - REST/SOAP Only
```properties
spring.profiles.active=kafka-disabled,activemq-disabled,ibmmq-disabled,tibco-disabled
```

## Troubleshooting

### 1. Profile Not Working

Check active profiles:
```bash
curl http://localhost:8080/actuator/env | grep profiles
```

### 2. UI Still Shows Enabled

- Check browser cache/refresh
- Verify API endpoint returns correct status
- Check network tab for API calls

### 3. Unexpected Bean Loading

- Verify `@Profile` annotations are correct
- Check for missing annotations on related beans
- Review Spring Boot startup logs for loaded beans

## Implementation Notes

1. **Thread Safety**: All profile checks are done at startup
2. **Performance**: No runtime profile checking overhead
3. **Consistency**: Frontend and backend status always aligned
4. **Graceful Degradation**: No errors when accessing disabled protocols
5. **Extensibility**: Easy to add new protocols to the system 
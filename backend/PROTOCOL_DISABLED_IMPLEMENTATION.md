# Protocol Disabled Profiles Implementation

This document describes the implementation of protocol-specific disabled profiles that allow selective disabling of protocol support in the Service Virtualization Platform.

## Supported Disabled Profiles

- `kafka-disabled` - Disables Kafka protocol support
- `activemq-disabled` - Disables ActiveMQ protocol support  
- `ibmmq-disabled` - Disables IBM MQ protocol support
- `tibco-disabled` - Disables TIBCO EMS protocol support
- `rest-disabled` - Disables REST/HTTP protocol support
- `soap-disabled` - Disables SOAP protocol support

## Architecture Overview

The implementation follows an **inverted approach** where:
- All protocols are **enabled by default**
- Protocols can be **selectively disabled** using specific Spring profiles
- When a protocol is disabled, **no beans are loaded** for that protocol (resource optimization)
- The frontend gracefully handles disabled protocols with visual indicators

## Backend Implementation

### Profile Annotations Applied

All protocol-specific components have been annotated with `@Profile("!protocol-disabled")`:

#### Kafka Components (`@Profile("!kafka-disabled")`)
- `KafkaConfig` - All Kafka beans (producers, consumers, admin, templates)
- Controllers: `KafkaStubController`, `KafkaMessageController`, `KafkaWebhookTestController`  
- Services: `KafkaStubService`, `KafkaMessageService`, `KafkaTopicService`, `KafkaCallbackService`, `SchemaRegistryService`, `KafkaStubListenerService`
- Initializer: `KafkaStubInitializer`

#### ActiveMQ Components (`@Profile("!activemq-disabled")`)
- `ActiveMQConfig` - JMS connection factories and templates
- Controllers: `ActiveMQStubController`
- Services: `ActiveMQStubService`, `ActiveMQWebhookService`, `ActiveMQResponseService`
- Initializer: `ActiveMQStubInitializer`

#### IBM MQ Components (`@Profile("!ibmmq-disabled")`)
- `IBMMQConfig` - IBM MQ connection factories and JMS setup
- Controllers: `IBMMQStubController`
- Services: `IBMMQStubService`, `IBMMQWebhookService`, `IBMMQResponseService`
- Initializer: `IBMMQStubInitializer`

#### TIBCO EMS Components (`@Profile("!tibco-disabled")`)
- `TibcoConfig` - TIBCO EMS connection setup
- Controllers: `TibcoStubController`
- Services: `TibcoStubService`, `TibcoWebhookService`, `TibcoResponseService`

#### REST Components (`@Profile("!rest-disabled")`)
- `WireMockConfig` - WireMock server configuration
- Controllers: `RestStubController`
- Services: `RestStubService`, `WireMockAdminService`, `RestWebhookService`
- Initializer: `StubInitializer` (REST)

#### SOAP Components (`@Profile("!soap-disabled")`)
- Controllers: `SoapStubController`
- Services: `SoapStubServiceImpl`
- Initializer: `SoapStubInitializer`

### Protocol Status API

Enhanced `HealthController` with new endpoint:

```http
GET /api/health/protocols
```

**Response:**
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
    // ... other protocols
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Frontend Implementation

### Updated Components

#### 1. API Integration (`healthApi.ts`)
```typescript
interface ProtocolStatus {
  name: string;
  enabled: boolean;
  reason: string;
}

export const useGetProtocolStatusQuery = healthApi.injectEndpoints({
  endpoints: (builder) => ({
    getProtocolStatus: builder.query<ProtocolStatus[], void>({
      query: () => '/health/protocols',
      transformResponse: (response: { protocols: ProtocolStatus[] }) => response.protocols,
    }),
  }),
}).useGetProtocolStatusQuery;
```

#### 2. Main Dashboard (`Dashboard.tsx`)
- Replaced hardcoded protocol status with real-time API data
- Added loading states and error handling
- Protocols show as enabled/disabled based on backend profiles

#### 3. Protocol Dashboards (e.g., `KafkaDashboard.tsx`)
- Detect disabled state from API
- Show informative message when protocol is disabled
- Graceful degradation of functionality

### Visual Indicators

**Enabled Protocols:**
- Normal card appearance
- Blue "Active" badge
- Functional action buttons

**Disabled Protocols:**
- Grayed out card with reduced opacity
- Red "Disabled" badge
- "Unavailable" buttons (non-functional)
- Informative tooltip explaining the disabled state

## Configuration Files

### Application Property Files

Each protocol has a dedicated configuration file for testing:

- `application-kafka-disabled.properties`
- `application-activemq-disabled.properties`
- `application-ibmmq-disabled.properties` 
- `application-tibco-disabled.properties`
- `application-rest-disabled.properties`
- `application-soap-disabled.properties`

**Example (`application-kafka-disabled.properties`):**
```properties
# Kafka Protocol Disabled Configuration
spring.application.name=Service Virtualization Platform - Kafka Disabled
spring.profiles.active=kafka-disabled,mongodb

# Reduced logging for disabled protocol
logging.level.com.service.virtualization.kafka=WARN
logging.level.org.apache.kafka=WARN
```

## Usage Examples

### 1. Disable Kafka Protocol
```bash
# Using profile-specific property file
java -jar app.jar --spring.config.location=application-kafka-disabled.properties

# Using command line
java -jar app.jar --spring.profiles.active=kafka-disabled,mongodb

# Using environment variable
export SPRING_PROFILES_ACTIVE=kafka-disabled,mongodb
java -jar app.jar
```

### 2. Disable Multiple Protocols
```bash
# Disable both Kafka and ActiveMQ
java -jar app.jar --spring.profiles.active=kafka-disabled,activemq-disabled,mongodb

# Disable all messaging protocols, keep REST/SOAP
java -jar app.jar --spring.profiles.active=kafka-disabled,activemq-disabled,ibmmq-disabled,tibco-disabled,mongodb
```

### 3. Testing Specific Protocol
```bash
# Test with only REST enabled
java -jar app.jar --spring.profiles.active=kafka-disabled,activemq-disabled,ibmmq-disabled,tibco-disabled,soap-disabled,mongodb
```

## Benefits

### 1. Resource Optimization
- **Memory**: Disabled protocols don't load beans, reducing memory footprint
- **CPU**: No background processing for disabled protocols
- **Network**: No connection attempts to disabled protocol infrastructure
- **Startup Time**: Faster application startup with fewer beans to initialize

### 2. Development & Testing
- **Isolation**: Test specific protocols without interference
- **Environment-Specific**: Different environments can have different protocol requirements
- **Debugging**: Easier to troubleshoot when unnecessary protocols are disabled

### 3. Deployment Flexibility
- **Microservice Architecture**: Different instances can handle different protocols
- **Cloud Native**: Better resource utilization in containerized environments
- **Cost Optimization**: Reduce licensing costs for unused protocol infrastructure

## Error Handling

### Common Issues

1. **Repository Bean Errors**: When using disabled profiles, ensure you also specify a repository profile (e.g., `mongodb` or `sybase`)
```bash
# ❌ Incorrect - missing repository profile
java -jar app.jar --spring.profiles.active=kafka-disabled

# ✅ Correct - includes repository profile  
java -jar app.jar --spring.profiles.active=kafka-disabled,mongodb
```

2. **Profile Conflicts**: Be aware of profile inheritance and conflicts
3. **Frontend Errors**: Ensure frontend handles API failures gracefully

### Monitoring

- Monitor `/api/health/protocols` endpoint for protocol status
- Check application logs for profile activation messages
- Verify expected beans are not loaded using Spring Boot Actuator endpoints

## Extension Guide

To add a new protocol disabled profile:

### Backend Steps
1. Add `@Profile("!newprotocol-disabled")` to all relevant components
2. Update `HealthController.getProtocolStatus()` to include the new protocol
3. Create `application-newprotocol-disabled.properties`

### Frontend Steps  
1. Protocol will automatically appear in dashboard via API
2. Add specific handling in protocol dashboard if needed
3. Update any hardcoded protocol lists

This implementation provides a robust, scalable foundation for protocol management that can easily be extended to support additional protocols in the future. 
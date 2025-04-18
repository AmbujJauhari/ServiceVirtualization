# Service Virtualization Backend

Backend for the Service Virtualization Platform with multi-protocol support.

## Features

- **Multi-Protocol Service Virtualization**: Create and manage stubs for various protocols:
  - HTTP/HTTPS (using WireMock)
  - gRPC (planned)
  - MQTT (planned)
  - WebSockets (planned)
  - AMQP (planned)
- **API Recording**: Record interactions with existing services to create stubs
- **Dual Mode Operation**: Acts as both a direct API endpoint and a proxy
- **Flexible Storage**: Store stubs and recordings in MongoDB or Sybase
- **Extensible Architecture**: Easy to extend with new protocols

## Architecture

The backend uses a modular architecture to support multiple protocols:

- **Core Domain Models**: Protocol-agnostic models for stubs and recordings
- **Protocol Abstraction Layer**: Interface for handling protocol-specific operations
- **Protocol Handlers**: Implementation of protocol-specific virtualization logic
- **Factory Pattern**: Dynamic selection of appropriate handlers based on protocol

## Technology Stack

- Java 17
- Spring Boot 3
- WireMock (for HTTP virtualization)
- MongoDB
- Sybase

## Getting Started

### Prerequisites

- JDK 17+
- Maven 3.6+
- MongoDB (for MongoDB storage)
- Sybase (for Sybase storage)

### Building the Application

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

Or using the JAR:

```bash
java -jar target/service-virtualization-backend-1.0.0-SNAPSHOT.jar
```

### Configuration

The application can be configured through the `application.yml` file:

- **Database**: Choose between MongoDB and Sybase
- **Protocol Handlers**: Configure settings for each protocol
- **REST Paths**: Configure API and proxy paths

Example configuration for protocols:

```yaml
app:
  protocol:
    config:
      http:
        port: 8081
        fileStoragePath: ./wiremock-storage
      https:
        port: 8443
        fileStoragePath: ./wiremock-storage
        keyStorePath: ./keystore.jks
        keyStorePassword: password
      mqtt:
        brokerUrl: tcp://localhost:1883
        clientId: service-virtualization
```

## API Documentation

API documentation is available via Swagger UI at `/swagger-ui.html` when the application is running.

## Project Structure

- `/src/main/java/com/service/virtualization`
  - `/config`: Application configurations
  - `/controller`: REST API endpoints
  - `/model`: Protocol-agnostic data models
  - `/protocol`: Protocol abstraction and implementation
  - `/repository`: Database repositories
  - `/service`: Business logic services
  - `/proxy`: Proxy handling

## Extending with New Protocols

To add support for a new protocol:

1. Create a new implementation of the `ProtocolHandler` interface:
```java
@Component
public class MyProtocolHandler implements ProtocolHandler {
    @Override
    public Protocol getProtocol() {
        return Protocol.MY_PROTOCOL;
    }
    
    // Implement the rest of the interface methods
}
```

2. Add the protocol to the `Protocol` enum:
```java
public enum Protocol {
    HTTP,
    HTTPS,
    MY_PROTOCOL;
}
```

3. Add configuration in the `application.yml` file:
```yaml
app:
  protocol:
    config:
      my_protocol:
        customSetting1: value1
        customSetting2: value2
```

The ProtocolHandlerFactory will automatically discover and initialize your protocol handler.

## Database Setup

### MongoDB

The application uses MongoDB for storing stubs and recordings. Ensure MongoDB is running on the configured URL.

### Sybase

For Sybase storage, ensure the database is properly set up with the following schemas:

```sql
CREATE TABLE stubs (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    service_path VARCHAR(255) NOT NULL,
    behind_proxy BIT DEFAULT 0,
    method VARCHAR(10) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    wiremock_mapping_id VARCHAR(36),
    status VARCHAR(20) NOT NULL,
    request_headers TEXT,
    response_headers TEXT,
    request_body TEXT,
    response_body TEXT,
    response_status INT DEFAULT 200
);

CREATE TABLE recordings (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    service_path VARCHAR(255) NOT NULL,
    behind_proxy BIT DEFAULT 0,
    method VARCHAR(10) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    recorded_at DATETIME NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    converted_to_stub BIT DEFAULT 0,
    converted_stub_id VARCHAR(36),
    request_headers TEXT,
    response_headers TEXT,
    request_body TEXT,
    response_body TEXT,
    response_status INT,
    source_ip VARCHAR(50)
);
``` 
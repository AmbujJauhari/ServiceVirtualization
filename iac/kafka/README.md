# Kafka Container for Service Virtualization

This directory contains Docker configurations for running Kafka in a containerized environment for use with the Service Virtualization application.

## Quick Start

```bash
# Go to the iac directory
cd iac

# Start the Kafka containers
docker-compose -f docker-compose-kafka.yml up -d
```

The Kafka services will be available at:
- Kafka broker: `localhost:29092` (for external connections)
- Zookeeper: `localhost:2181`
- Kafka UI: `http://localhost:8080` (web-based management interface)

## Components

The deployment includes several containers:

1. **Zookeeper**: Used for Kafka's coordination and metadata storage
2. **Kafka Broker**: The core message broker
3. **Kafka Init**: Initialization container that sets up the required topics
4. **Kafka UI**: Web-based management interface for monitoring and managing Kafka

## Default Topics

The following topics are automatically created during initialization:

- `virtualization.requests`: For incoming requests
- `virtualization.responses`: For outgoing responses
- `virtualization.errors`: For error messages
- `virtualization.events`: For system events

## Configuration

### Environment Variables

You can modify the following environment variables in the `docker-compose-kafka.yml` file:

- `KAFKA_ADVERTISED_LISTENERS`: Configures how clients connect to Kafka
- `KAFKA_LOG_RETENTION_HOURS`: How long messages are retained (default: 168 hours / 7 days)
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE`: Whether topics can be automatically created

### Custom Configuration

The Kafka configuration is stored in the `kafka/config` directory:

- `server.properties`: Main configuration file for Kafka
- `zookeeper.properties`: Configuration for Zookeeper
- `create-topics.sh`: Script for creating default topics

## Integration with Service Virtualization

To configure your Service Virtualization application to use this Kafka container, update your application properties:

```properties
kafka.bootstrap-servers=localhost:29092
```

## Working with Kafka

### Creating Topics

You can create additional topics using the Kafka UI or the command line:

```bash
docker exec sv-kafka kafka-topics --bootstrap-server kafka:9092 --create --topic my-topic --partitions 3 --replication-factor 1
```

### Producing Messages

Send messages to a topic:

```bash
docker exec -it sv-kafka kafka-console-producer --bootstrap-server kafka:9092 --topic virtualization.requests
```

### Consuming Messages

Read messages from a topic:

```bash
docker exec -it sv-kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic virtualization.responses --from-beginning
```

## Data Persistence

Kafka and Zookeeper data are persisted in Docker volumes:
- `sv-kafka-data`: Kafka message data
- `sv-zookeeper-data`: Zookeeper data
- `sv-zookeeper-log`: Zookeeper transaction logs

This ensures that your configurations and messages are preserved across container restarts.

## Troubleshooting

- If the containers fail to start, check the logs:
  ```bash
  docker-compose -f docker-compose-kafka.yml logs
  ```

- If you can't connect to Kafka, ensure ports are not in use by another application:
  ```bash
  netstat -an | grep 9092
  ```

- To restart the Kafka stack:
  ```bash
  docker-compose -f docker-compose-kafka.yml restart
  ```

- To completely reset Kafka (including all data):
  ```bash
  docker-compose -f docker-compose-kafka.yml down -v
  docker-compose -f docker-compose-kafka.yml up -d
  ``` 
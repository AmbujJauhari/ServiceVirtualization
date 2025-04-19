# Service Virtualization Infrastructure

This directory contains Docker configurations for running the infrastructure required by the Service Virtualization application.

## Available Services

### ActiveMQ

Apache ActiveMQ is a powerful open source messaging server. It supports multiple protocols including JMS, AMQP, STOMP, and MQTT.

[See ActiveMQ documentation](activemq/README.md)

Quick start:
```bash
# Start ActiveMQ
docker-compose up -d
```

Access at:
- JMS endpoint: `tcp://localhost:61616`
- Web console: `http://localhost:8161/admin` (credentials: admin/admin)

### Kafka

Apache Kafka is a distributed event streaming platform capable of handling trillions of events a day.

[See Kafka documentation](kafka/README.md)

Quick start:
```bash
# Start Kafka
docker-compose -f docker-compose-kafka.yml up -d
```

Access at:
- Kafka: `localhost:29092`
- Kafka UI: `http://localhost:8080`

### MongoDB

MongoDB is a document-oriented NoSQL database used for high volume data storage.

[See MongoDB documentation](mongodb/README.md)

Quick start:
```bash
# Start MongoDB
docker-compose -f docker-compose-mongodb.yml up -d
```

Access at:
- MongoDB: `mongodb://localhost:27017`
- MongoDB Express: `http://localhost:8081` (credentials: admin/password)

## Running Multiple Services

You can run multiple services simultaneously:

```bash
# Start all services
docker-compose up -d
docker-compose -f docker-compose-kafka.yml up -d
docker-compose -f docker-compose-mongodb.yml up -d
```

## Stopping Services

To stop the services:

```bash
# Stop services
docker-compose down
docker-compose -f docker-compose-kafka.yml down
docker-compose -f docker-compose-mongodb.yml down
```

Add `-v` flag to remove volumes and completely reset the services:

```bash
docker-compose down -v
```

## Configuration

### Environment Variables

See the individual service READMEs for specific environment variables you can configure.

## Integration with Service Virtualization

To configure your Service Virtualization application to use this ActiveMQ container, update your application properties:

```properties
activemq.broker-url=tcp://localhost:61616
activemq.username=admin
activemq.password=admin
```

## Data Persistence

ActiveMQ data is persisted in a Docker volume named `sv-activemq-data`. This ensures that your messages and configuration are preserved across container restarts.

## Monitoring

### JMX Access

JMX is exposed on port 1099. You can connect using tools like JConsole:

```bash
jconsole localhost:1099
```

### Web Console

The ActiveMQ web console provides a graphical interface for monitoring and managing the broker. Access it at:

```
http://localhost:8161/admin
```

Default credentials: admin/admin

## Troubleshooting

- If the container fails to start, check the logs:
  ```bash
  docker-compose logs activemq
  ```

- If you can't connect to ActiveMQ, ensure ports are not in use by another application:
  ```bash
  netstat -an | grep 61616
  ```

- To restart the ActiveMQ container:
  ```bash
  docker-compose restart activemq
  ``` 
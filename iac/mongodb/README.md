# MongoDB Container for Service Virtualization

This directory contains Docker configurations for running MongoDB in a containerized environment for use with the Service Virtualization application.

## Quick Start

```bash
# Go to the iac directory
cd iac

# Start the MongoDB containers
docker-compose -f docker-compose-mongodb.yml up -d
```

The MongoDB services will be available at:
- MongoDB server: `mongodb://localhost:27017`
- MongoDB Express UI: `http://localhost:8081` (credentials: admin/password)

## Components

The deployment includes several containers:

1. **MongoDB**: The core document database
2. **MongoDB Express**: Web-based management interface
3. **MongoDB Init**: Initialization container that sets up the required collections and indexes

## Default Collections

The following collections are automatically created during initialization:

- `rest_stubs`: For REST API stub configurations
- `soap_stubs`: For SOAP API stub configurations
- `kafka_stubs`: For Kafka message stub configurations
- `activemq_stubs`: For ActiveMQ message stub configurations
- `tibco_stubs`: For TIBCO message stub configurations
- `ibmmq_stubs`: For IBM MQ message stub configurations
- `file_stubs`: For file stub configurations
- `users`: For user management
- `usage_statistics`: For tracking usage metrics

## Configuration

### Environment Variables

You can modify the following environment variables in the `docker-compose-mongodb.yml` file:

- `MONGO_INITDB_ROOT_USERNAME`: MongoDB admin username
- `MONGO_INITDB_ROOT_PASSWORD`: MongoDB admin password
- `MONGO_INITDB_DATABASE`: Default database name
- `ME_CONFIG_BASICAUTH_USERNAME`: MongoDB Express login username
- `ME_CONFIG_BASICAUTH_PASSWORD`: MongoDB Express login password

### Custom Configuration

The MongoDB configuration is stored in the `mongodb/config` directory:

- `mongod.conf`: Main configuration file for MongoDB

## Integration with Service Virtualization

To configure your Service Virtualization application to use this MongoDB container, update your application properties:

```properties
spring.data.mongodb.uri=mongodb://admin:password@localhost:27017/service_virtualization
```

## Working with MongoDB

### Connecting via Command Line

You can connect to the MongoDB instance from the command line:

```bash
docker exec -it sv-mongodb mongosh mongodb://admin:password@localhost:27017/service_virtualization
```

### Creating Collections Manually

Create collections via the MongoDB Express UI or command line:

```bash
docker exec -it sv-mongodb mongosh admin:password@localhost:27017/service_virtualization --eval "db.createCollection('new_collection')"
```

### Backing Up Data

Backup your MongoDB data:

```bash
docker exec -it sv-mongodb mongodump --uri=mongodb://admin:password@localhost:27017/service_virtualization --out=/data/backup
```

### Restoring Data

Restore your MongoDB data:

```bash
docker exec -it sv-mongodb mongorestore --uri=mongodb://admin:password@localhost:27017/service_virtualization /data/backup
```

## Data Persistence

MongoDB data is persisted in a Docker volume:
- `sv-mongodb-data`: MongoDB data files

This ensures that your data is preserved across container restarts.

## Troubleshooting

- If the containers fail to start, check the logs:
  ```bash
  docker-compose -f docker-compose-mongodb.yml logs
  ```

- If you can't connect to MongoDB, ensure ports are not in use by another application:
  ```bash
  netstat -an | grep 27017
  ```

- To restart the MongoDB stack:
  ```bash
  docker-compose -f docker-compose-mongodb.yml restart
  ```

- To completely reset MongoDB (including all data):
  ```bash
  docker-compose -f docker-compose-mongodb.yml down -v
  docker-compose -f docker-compose-mongodb.yml up -d
  ``` 
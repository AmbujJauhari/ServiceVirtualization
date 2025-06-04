# Service Virtualization Infrastructure

This directory contains Docker Compose configurations for deploying the complete Service Virtualization infrastructure, including message queues, databases, and management tools.

## 🏗️ Architecture Overview

The infrastructure includes:

- **Message Queues**: IBM MQ, Apache Kafka, ActiveMQ
- **Databases**: MongoDB, Sybase ASE
- **Management Tools**: Web consoles for all services
- **Monitoring**: Kafka UI, MongoDB Express

## 📋 Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 8GB RAM available for containers
- Ports 1414, 5000, 8080-8082, 9080, 9092, 9443, 27017, 61616 available

## 🚀 Quick Start

### Option 1: Deploy All Services
```bash
# Deploy complete infrastructure
docker-compose -f docker-compose-all.yml up -d

# Check status
docker-compose -f docker-compose-all.yml ps

# View logs
docker-compose -f docker-compose-all.yml logs -f
```

### Option 2: Deploy Individual Services

#### IBM MQ Only
```bash
docker-compose -f docker-compose-ibmmq.yml up -d
```

#### Kafka Only
```bash
docker-compose -f docker-compose-kafka.yml up -d
```

## 🔧 Service Details

### IBM MQ
- **Container**: `sv-ibmmq`
- **Ports**: 
  - 1414 (MQ Listener)
  - 9443 (HTTPS Console)
  - 9080 (HTTP Console - alternative)
  - 9157 (Metrics)
- **Queue Manager**: QM1
- **Credentials**: admin/passw0rd
- **Web Console**: https://localhost:9443/ibmmq/console

#### Pre-configured Queues
- `DEV.REQUEST.QUEUE` - Development request queue
- `DEV.RESPONSE.QUEUE` - Development response queue
- `SV.STUB.REQUEST` - Service virtualization stub requests
- `SV.STUB.RESPONSE` - Service virtualization stub responses
- `TEST.PRIORITY.HIGH` - High priority test queue
- `TEST.JSON.QUEUE` - JSON message test queue
- `TEST.XML.QUEUE` - XML message test queue

#### Pre-configured Channels
- `DEV.ADMIN.SVRCONN` - Admin server connection
- `SV.APP.SVRCONN` - Application server connection

### Apache Kafka
- **Container**: `sv-kafka`
- **Ports**: 9092 (Bootstrap server)
- **Zookeeper**: `sv-zookeeper` (port 2181)
- **UI**: http://localhost:8082

### MongoDB
- **Container**: `sv-mongodb`
- **Port**: 27017
- **Credentials**: admin/password
- **Database**: service_virtualization
- **Web UI**: http://localhost:8081 (MongoDB Express)

### Sybase ASE
- **Container**: `sv-sybase`
- **Port**: 5000
- **Credentials**: sa/myPassword
- **Database**: service_virtualization

### ActiveMQ
- **Container**: `sv-activemq`
- **Ports**: 
  - 61616 (OpenWire)
  - 8161 (Web Console)
  - 5672 (AMQP)
  - 61613 (STOMP)
  - 1883 (MQTT)
- **Credentials**: admin/admin
- **Web Console**: http://localhost:8161

## 🔐 Security Configuration

### IBM MQ Security
- Default users: `admin`, `app`, `svapp`
- Channel authentication configured
- Queue-level permissions set
- TLS/SSL ready (certificates can be mounted)

### Default Credentials
| Service | Username | Password | Notes |
|---------|----------|----------|-------|
| IBM MQ | admin | passw0rd | Full admin access |
| IBM MQ | app | passw0rd | Application access |
| MongoDB | admin | password | Root access |
| Sybase | sa | myPassword | System admin |
| ActiveMQ | admin | admin | Admin console |
| Kafka UI | - | - | No authentication |

## 📁 Directory Structure

```
iac/
├── docker-compose-all.yml      # Complete infrastructure
├── docker-compose-ibmmq.yml    # IBM MQ only
├── docker-compose-kafka.yml    # Kafka only
├── ibmmq/
│   ├── config/
│   │   └── mq.ini              # MQ configuration
│   └── scripts/
│       ├── setup-queues.mqsc   # Queue setup script
│       └── init-container.sh   # Container initialization
├── kafka/
│   └── config/
├── mongodb/
│   └── init/
└── README.md                   # This file
```

## 🛠️ Management Commands

### IBM MQ Commands
```bash
# Connect to IBM MQ container
docker exec -it sv-ibmmq bash

# Display queue manager
dspmq

# Run MQ commands
runmqsc QM1

# Display queues
echo "DISPLAY QLOCAL(*)" | runmqsc QM1

# Display channels
echo "DISPLAY CHANNEL(*)" | runmqsc QM1
```

### Kafka Commands
```bash
# List topics
docker exec sv-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Create topic
docker exec sv-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic

# Produce messages
docker exec -it sv-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic test-topic

# Consume messages
docker exec -it sv-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

### MongoDB Commands
```bash
# Connect to MongoDB
docker exec -it sv-mongodb mongosh -u admin -p password

# List databases
show dbs

# Use service virtualization database
use service_virtualization

# List collections
show collections
```

## 🔍 Monitoring and Logs

### View Service Logs
```bash
# All services
docker-compose -f docker-compose-all.yml logs -f

# Specific service
docker-compose -f docker-compose-all.yml logs -f ibmmq

# IBM MQ error logs
docker exec sv-ibmmq cat /var/mqm/qmgrs/QM1/errors/AMQERR01.LOG
```

### Health Checks
```bash
# Check all container status
docker ps

# Check IBM MQ health
docker exec sv-ibmmq dspmq -m QM1

# Check Kafka health
docker exec sv-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

## 🚨 Troubleshooting

### Common Issues

#### IBM MQ Won't Start
```bash
# Check logs
docker logs sv-ibmmq

# Verify license acceptance
docker exec sv-ibmmq env | grep LICENSE

# Check queue manager status
docker exec sv-ibmmq dspmq -m QM1
```

#### Port Conflicts
```bash
# Check port usage
netstat -tulpn | grep :1414

# Stop conflicting services
sudo systemctl stop ibm-mq  # if installed locally
```

#### Memory Issues
```bash
# Check container memory usage
docker stats

# Increase Docker memory limit in Docker Desktop
# Or reduce container resource limits in compose files
```

### Reset Environment
```bash
# Stop all services
docker-compose -f docker-compose-all.yml down

# Remove volumes (WARNING: This deletes all data)
docker-compose -f docker-compose-all.yml down -v

# Remove images
docker-compose -f docker-compose-all.yml down --rmi all

# Clean up
docker system prune -f
```

## 🔧 Customization

### Adding Custom Queues
1. Edit `iac/ibmmq/scripts/setup-queues.mqsc`
2. Add your queue definitions
3. Restart IBM MQ container

### Custom Configuration
1. Mount custom config files in compose volumes
2. Override environment variables
3. Use Docker secrets for production credentials

### Production Considerations
- Use external volumes for data persistence
- Configure proper TLS/SSL certificates
- Set up proper authentication and authorization
- Configure monitoring and alerting
- Use Docker secrets for sensitive data
- Set resource limits appropriately

## 📚 Additional Resources

- [IBM MQ Documentation](https://www.ibm.com/docs/en/ibm-mq)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [MongoDB Documentation](https://docs.mongodb.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## 🤝 Support

For issues related to:
- **Infrastructure**: Check this README and Docker logs
- **Service Virtualization**: See main application documentation
- **IBM MQ**: Consult IBM MQ documentation
- **Kafka**: Check Kafka documentation 
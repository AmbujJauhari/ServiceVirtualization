#!/bin/bash
# Complete Service Virtualization Infrastructure Startup Script

set -e

echo "🚀 Starting Complete Service Virtualization Infrastructure..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check available memory
available_memory=$(docker system info --format '{{.MemTotal}}' 2>/dev/null || echo "0")
if [ "$available_memory" -lt 8000000000 ]; then
    echo "⚠️  Warning: Less than 8GB memory available for Docker. Some services may fail to start."
    echo "   Consider increasing Docker memory limit or stopping other applications."
fi

# Create network if it doesn't exist
if ! docker network ls | grep -q sv-network; then
    echo "🔗 Creating sv-network..."
    docker network create sv-network
fi

# Make scripts executable
chmod +x ibmmq/scripts/*.sh 2>/dev/null || true

echo "📋 Starting services in order..."

# Start databases first
echo "🗄️  Starting databases..."
docker-compose -f docker-compose-all.yml up -d mongodb sybase

# Wait for databases
echo "⏳ Waiting for databases to be ready..."
sleep 30

# Start message queues
echo "📨 Starting message queues..."
docker-compose -f docker-compose-all.yml up -d zookeeper kafka ibmmq activemq

# Wait for message queues
echo "⏳ Waiting for message queues to be ready..."
sleep 60

# Start management tools
echo "🛠️  Starting management tools..."
docker-compose -f docker-compose-all.yml up -d kafka-ui ibmmq-console mongo-express

# Wait for all services to be healthy
echo "⏳ Waiting for all services to be ready..."
timeout=600  # 10 minutes timeout
elapsed=0
interval=15

services_to_check=("sv-mongodb" "sv-kafka" "sv-ibmmq" "sv-activemq")

while [ $elapsed -lt $timeout ]; do
    all_ready=true
    
    for service in "${services_to_check[@]}"; do
        if ! docker ps --filter name=$service --filter status=running | grep -q $service; then
            all_ready=false
            echo "   Waiting for $service... (${elapsed}s elapsed)"
            break
        fi
    done
    
    # Special check for IBM MQ
    if [ "$all_ready" = true ]; then
        if ! docker exec sv-ibmmq dspmq -m QM1 2>/dev/null | grep -q "RUNNING"; then
            all_ready=false
            echo "   Waiting for IBM MQ Queue Manager... (${elapsed}s elapsed)"
        fi
    fi
    
    if [ "$all_ready" = true ]; then
        echo "✅ All services are ready!"
        break
    fi
    
    sleep $interval
    elapsed=$((elapsed + interval))
done

if [ $elapsed -ge $timeout ]; then
    echo "❌ Timeout waiting for services to start"
    echo "📋 Service status:"
    docker-compose -f docker-compose-all.yml ps
    exit 1
fi

# Initialize IBM MQ if script exists
if [ -f "ibmmq/scripts/init-container.sh" ]; then
    echo "🔧 Initializing IBM MQ..."
    docker exec sv-ibmmq bash /opt/mqm/scripts/init-container.sh 2>/dev/null || echo "⚠️  IBM MQ initialization completed with warnings"
fi

# Display comprehensive status
echo ""
echo "🎉 Service Virtualization Infrastructure is now running!"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "📊 SERVICE STATUS"
echo "═══════════════════════════════════════════════════════════════"
docker-compose -f docker-compose-all.yml ps

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "🔗 CONNECTION DETAILS"
echo "═══════════════════════════════════════════════════════════════"

echo ""
echo "📨 MESSAGE QUEUES:"
echo "   IBM MQ:"
echo "     - Host: localhost:1414"
echo "     - Queue Manager: QM1"
echo "     - Admin Channel: DEV.ADMIN.SVRCONN"
echo "     - App Channel: SV.APP.SVRCONN"
echo "     - Credentials: admin/passw0rd"
echo ""
echo "   Apache Kafka:"
echo "     - Bootstrap Server: localhost:9092"
echo "     - Zookeeper: localhost:2181"
echo ""
echo "   ActiveMQ:"
echo "     - Broker URL: tcp://localhost:61616"
echo "     - Credentials: admin/admin"

echo ""
echo "🗄️  DATABASES:"
echo "   MongoDB:"
echo "     - Connection: mongodb://admin:password@localhost:27017/service_virtualization"
echo "     - Database: service_virtualization"
echo ""
echo "   Sybase ASE:"
echo "     - Host: localhost:5000"
echo "     - Database: service_virtualization"
echo "     - Credentials: sa/myPassword"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "🌐 WEB CONSOLES"
echo "═══════════════════════════════════════════════════════════════"
echo "   IBM MQ Console (HTTPS): https://localhost:9443/ibmmq/console"
echo "   IBM MQ Console (HTTP):  http://localhost:9080"
echo "   Kafka UI:               http://localhost:8082"
echo "   ActiveMQ Console:       http://localhost:8161"
echo "   MongoDB Express:        http://localhost:8081"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "🛠️  MANAGEMENT COMMANDS"
echo "═══════════════════════════════════════════════════════════════"
echo "   View all logs:          docker-compose -f docker-compose-all.yml logs -f"
echo "   View specific service:   docker-compose -f docker-compose-all.yml logs -f [service]"
echo "   Stop all services:      docker-compose -f docker-compose-all.yml down"
echo "   Reset environment:      docker-compose -f docker-compose-all.yml down -v"
echo "   Container shell:        docker exec -it [container-name] bash"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "🔍 HEALTH CHECKS"
echo "═══════════════════════════════════════════════════════════════"

# Quick health checks
echo "   IBM MQ Status:"
if docker exec sv-ibmmq dspmq -m QM1 2>/dev/null | grep -q "RUNNING"; then
    echo "     ✅ Queue Manager QM1 is RUNNING"
else
    echo "     ❌ Queue Manager QM1 is not running"
fi

echo "   Kafka Status:"
if docker exec sv-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; then
    echo "     ✅ Kafka broker is responding"
else
    echo "     ❌ Kafka broker is not responding"
fi

echo "   MongoDB Status:"
if docker exec sv-mongodb mongosh --quiet --eval "db.runCommand('ping')" >/dev/null 2>&1; then
    echo "     ✅ MongoDB is responding"
else
    echo "     ❌ MongoDB is not responding"
fi

echo ""
echo "🎯 Infrastructure is ready for Service Virtualization!"
echo "   You can now start your backend application and connect to these services."
echo "" 
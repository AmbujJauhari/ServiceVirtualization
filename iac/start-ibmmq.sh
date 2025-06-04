#!/bin/bash
# IBM MQ Startup Script for Service Virtualization

set -e

echo "🚀 Starting IBM MQ for Service Virtualization..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Create network if it doesn't exist
if ! docker network ls | grep -q sv-network; then
    echo "🔗 Creating sv-network..."
    docker network create sv-network
fi

# Make scripts executable
chmod +x ibmmq/scripts/*.sh

# Start IBM MQ
echo "🔧 Starting IBM MQ container..."
docker-compose -f docker-compose-ibmmq.yml up -d

# Wait for IBM MQ to be healthy
echo "⏳ Waiting for IBM MQ to be ready..."
timeout=300  # 5 minutes timeout
elapsed=0
interval=10

while [ $elapsed -lt $timeout ]; do
    if docker exec sv-ibmmq dspmq -m QM1 2>/dev/null | grep -q "RUNNING"; then
        echo "✅ IBM MQ is ready!"
        break
    fi
    
    echo "   Still waiting... (${elapsed}s elapsed)"
    sleep $interval
    elapsed=$((elapsed + interval))
done

if [ $elapsed -ge $timeout ]; then
    echo "❌ Timeout waiting for IBM MQ to start"
    echo "📋 Container logs:"
    docker logs sv-ibmmq --tail 50
    exit 1
fi

# Run initialization script
echo "🔧 Running initialization script..."
if [ -f "ibmmq/scripts/init-container.sh" ]; then
    docker exec sv-ibmmq bash /opt/mqm/scripts/init-container.sh
else
    echo "⚠️  Initialization script not found, running basic setup..."
    docker exec sv-ibmmq runmqsc QM1 << 'EOF'
DEFINE QLOCAL(DEV.REQUEST.QUEUE) DESCR('Development Request Queue') MAXDEPTH(5000) DEFPSIST(YES)
DEFINE QLOCAL(DEV.RESPONSE.QUEUE) DESCR('Development Response Queue') MAXDEPTH(5000) DEFPSIST(YES)
DEFINE QLOCAL(SV.STUB.REQUEST) DESCR('Service Virtualization Stub Request Queue') MAXDEPTH(10000) DEFPSIST(YES)
DEFINE QLOCAL(SV.STUB.RESPONSE) DESCR('Service Virtualization Stub Response Queue') MAXDEPTH(10000) DEFPSIST(YES)
DEFINE CHANNEL(DEV.ADMIN.SVRCONN) CHLTYPE(SVRCONN) DESCR('Development Admin Server Connection') REPLACE
DEFINE CHANNEL(SV.APP.SVRCONN) CHLTYPE(SVRCONN) DESCR('Service Virtualization Application Channel') REPLACE
SET CHLAUTH(DEV.ADMIN.SVRCONN) TYPE(BLOCKUSER) USERLIST('nobody')
SET CHLAUTH(SV.APP.SVRCONN) TYPE(BLOCKUSER) USERLIST('nobody')
START CHANNEL(DEV.ADMIN.SVRCONN)
START CHANNEL(SV.APP.SVRCONN)
EOF
fi

# Display status
echo ""
echo "🎉 IBM MQ is now running!"
echo ""
echo "📝 Connection Details:"
echo "   Queue Manager: QM1"
echo "   Host: localhost"
echo "   Port: 1414"
echo "   Admin Channel: DEV.ADMIN.SVRCONN"
echo "   App Channel: SV.APP.SVRCONN"
echo ""
echo "🔐 Default Credentials:"
echo "   Admin User: admin"
echo "   Admin Password: passw0rd"
echo "   App User: app"
echo "   App Password: passw0rd"
echo ""
echo "🌐 Web Consoles:"
echo "   HTTPS Console: https://localhost:9443/ibmmq/console"
echo "   HTTP Console:  http://localhost:9080"
echo ""
echo "📊 Container Status:"
docker ps --filter name=sv-ibmmq --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "🔍 To view logs: docker logs sv-ibmmq -f"
echo "🛠️  To stop: docker-compose -f docker-compose-ibmmq.yml down"
echo "" 
# IBM MQ Startup Script for Service Virtualization (PowerShell)

param(
    [switch]$WaitForReady = $true,
    [int]$TimeoutMinutes = 5
)

Write-Host "🚀 Starting IBM MQ for Service Virtualization..." -ForegroundColor Green

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "❌ Docker is not running. Please start Docker first." -ForegroundColor Red
    exit 1
}

# Create network if it doesn't exist
$networkExists = docker network ls | Select-String "sv-network"
if (-not $networkExists) {
    Write-Host "🔗 Creating sv-network..." -ForegroundColor Yellow
    docker network create sv-network
}

# Start IBM MQ
Write-Host "🔧 Starting IBM MQ container..." -ForegroundColor Yellow
docker-compose -f docker-compose-ibmmq.yml up -d

if ($WaitForReady) {
    # Wait for IBM MQ to be healthy
    Write-Host "⏳ Waiting for IBM MQ to be ready..." -ForegroundColor Yellow
    $timeout = $TimeoutMinutes * 60  # Convert to seconds
    $elapsed = 0
    $interval = 10

    while ($elapsed -lt $timeout) {
        try {
            $status = docker exec sv-ibmmq dspmq -m QM1 2>$null
            if ($status -match "RUNNING") {
                Write-Host "✅ IBM MQ is ready!" -ForegroundColor Green
                break
            }
        } catch {
            # Continue waiting
        }
        
        Write-Host "   Still waiting... ($elapsed s elapsed)" -ForegroundColor Gray
        Start-Sleep $interval
        $elapsed += $interval
    }

    if ($elapsed -ge $timeout) {
        Write-Host "❌ Timeout waiting for IBM MQ to start" -ForegroundColor Red
        Write-Host "📋 Container logs:" -ForegroundColor Yellow
        docker logs sv-ibmmq --tail 50
        exit 1
    }

    # Run initialization script
    Write-Host "🔧 Running initialization script..." -ForegroundColor Yellow
    if (Test-Path "ibmmq/scripts/init-container.sh") {
        docker exec sv-ibmmq bash /opt/mqm/scripts/init-container.sh
    } else {
        Write-Host "⚠️  Initialization script not found, running basic setup..." -ForegroundColor Yellow
        
        $mqscCommands = @"
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
"@
        
        $mqscCommands | docker exec -i sv-ibmmq runmqsc QM1
    }
}

# Display status
Write-Host ""
Write-Host "🎉 IBM MQ is now running!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Connection Details:" -ForegroundColor Cyan
Write-Host "   Queue Manager: QM1"
Write-Host "   Host: localhost"
Write-Host "   Port: 1414"
Write-Host "   Admin Channel: DEV.ADMIN.SVRCONN"
Write-Host "   App Channel: SV.APP.SVRCONN"
Write-Host ""
Write-Host "🔐 Default Credentials:" -ForegroundColor Cyan
Write-Host "   Admin User: admin"
Write-Host "   Admin Password: passw0rd"
Write-Host "   App User: app"
Write-Host "   App Password: passw0rd"
Write-Host ""
Write-Host "🌐 Web Consoles:" -ForegroundColor Cyan
Write-Host "   HTTPS Console: https://localhost:9443/ibmmq/console"
Write-Host "   HTTP Console:  http://localhost:9080"
Write-Host ""
Write-Host "📊 Container Status:" -ForegroundColor Cyan
docker ps --filter name=sv-ibmmq --format "table {{.Names}}`t{{.Status}}`t{{.Ports}}"
Write-Host ""
Write-Host "🔍 To view logs: docker logs sv-ibmmq -f" -ForegroundColor Yellow
Write-Host "🛠️  To stop: docker-compose -f docker-compose-ibmmq.yml down" -ForegroundColor Yellow
Write-Host "" 
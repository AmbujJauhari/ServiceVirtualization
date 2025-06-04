#!/bin/bash

# Wait for IBM MQ to be ready
sleep 10

# Create admin user for web console
echo "Creating admin user..."
useradd -m -d /home/admin admin 2>/dev/null || echo "User admin already exists"
echo "admin:admin" | chpasswd

# Add admin to mqm group if it exists
if getent group mqm > /dev/null 2>&1; then
    usermod -a -G mqm admin
    echo "Added admin to mqm group"
fi

# Configure web console for basic authentication
echo "Configuring web console authentication..."

# Create web authentication configuration
cat > /tmp/web-auth.xml << 'EOF'
<server>
    <featureManager>
        <feature>basicAuthenticationMQ-1.0</feature>
    </featureManager>
    
    <basicRegistry id="basic" realm="MQWebConsole">
        <user name="admin" password="admin"/>
    </basicRegistry>
    
    <authorization-roles id="adminRole">
        <security-role name="MQWebAdmin">
            <user name="admin"/>
        </security-role>
    </authorization-roles>
</server>
EOF

# Copy the configuration if possible
mkdir -p /mnt/mqm/data/web/installations/Installation1/servers/mqweb
cp /tmp/web-auth.xml /mnt/mqm/data/web/installations/Installation1/servers/mqweb/auth.xml 2>/dev/null || echo "Could not copy auth config"

echo "Web authentication configuration completed" 
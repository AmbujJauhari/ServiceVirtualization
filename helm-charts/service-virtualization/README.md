# Service Virtualization Platform - Helm Chart

Enterprise-grade Helm chart for deploying the Service Virtualization Platform with support for multiple messaging protocols and flexible configuration patterns.

## 🎯 **Features**

- ✅ **Core Platform**: Backend API + React UI with smart path-aware routing
- ✅ **Infrastructure Agnostic**: Works with any networking layer (Ingress, Service Mesh, Load Balancers)
- ✅ **Optional Services**: MongoDB, Kafka, TIBCO, ActiveMQ, IBM MQ (enable as needed)
- ✅ **Flexible Configuration**: ConfigMaps, init containers, or direct environment variables
- ✅ **Enterprise Ready**: Security contexts, resource limits, health checks
- ✅ **Production Optimized**: Resource management, scaling, health monitoring

## 🚀 **Quick Start**

### Local Development
```bash
# Deploy with local development configuration
helm install sv-platform . -f values-local-dev.yaml
```

### Production Deployment
```bash
# 1. Create your production values file
cp values-local-dev.yaml values-production.yaml

# 2. Edit with your configuration
vim values-production.yaml

# 3. Deploy
helm install sv-platform . -f values-production.yaml
```

## 📋 **Multi-Layer Configuration Architecture**

This chart uses a **layered configuration approach** to balance ease-of-use with enterprise flexibility:

### 🔷 **Backend Configuration Patterns**

#### Pattern 1: Direct Environment Variables
```yaml
# values.yaml
backend:
  env:
    MONGODB_URI: "mongodb://mongo.company.com:27017"
    TIBCO_URL: "tcp://tibco.company.com:7222"
    TIBCO_USERNAME: "sv-user"
    TIBCO_PASSWORD: "secret123"
```

#### Pattern 2: ConfigMap (Recommended)
```bash
# Create ConfigMap from file
kubectl create configmap sv-config --from-file=application.properties

# Deploy with ConfigMap
helm install sv-platform . --set backend.configMap.name=sv-config
```

#### Pattern 3: Init Container + Vault
```yaml
# values.yaml
backend:
  initContainers:
  - name: fetch-secrets
    image: "company.com/vault-fetcher:latest"
    command: ["/bin/sh"]
    args: ["-c", "fetch-from-azure-keyvault.sh"]
    volumeMounts:
    - name: config-volume
      mountPath: /app/config
  volumes:
  - name: config-volume
    emptyDir: {}
  volumeMounts:
  - name: config-volume
    mountPath: /app/config
```

---

### 🔷 **MongoDB: Three-Layer Configuration**

MongoDB supports a flexible three-layer configuration approach:

#### **Layer 1: Authentication (Secure) - Choose ONE method**

**Option A: Kubernetes Secret (Recommended for Production)**
```yaml
mongodb:
  enabled: true
  auth:
    method: "secret"
    existingSecret: "mongodb-credentials"
    keys:
      username: "username"
      password: "password"
      database: "database"
```

**Option B: Init Container (Enterprise - Azure Key Vault)**
```yaml
mongodb:
  enabled: true
  auth:
    method: "init-container"
    initContainers:
    - name: fetch-from-akv
      image: "mcr.microsoft.com/azure-cli:latest"
      command: ["/bin/sh", "-c"]
      args:
      - |
        az login --identity
        az keyvault secret show --vault-name prod-vault --name mongo-user --query value -o tsv > /shared/auth/username
        az keyvault secret show --vault-name prod-vault --name mongo-pass --query value -o tsv > /shared/auth/password
        az keyvault secret show --vault-name prod-vault --name mongo-db --query value -o tsv > /shared/auth/database
    paths:
      username: "/shared/auth/username"
      password: "/shared/auth/password"
      database: "/shared/auth/database"
```

#### **Layer 3: Additional Environment Variables (Optional Tuning)**
```yaml
mongodb:
  enabled: true
  # ... auth config ...
  env:
    MONGO_QUERY_TIMEOUT: "15000"
    MONGO_MAX_CONNECTIONS: "500"
    MONGO_LOG_LEVEL: "INFO"
```

---

### 🔷 **TIBCO: Four-Layer Configuration**

TIBCO introduces a unique **Layer 0: Bootstrap** for auto-generating configuration files!

#### **Layer 0: Bootstrap (Auto-Generate Config) - LOCAL DEV MAGIC! ✨**

Perfect for local development - just enable and go!

```yaml
tibco:
  enabled: true
  
  image:
    repository: kytay/tibco-ems
    tag: latest
  
  # 🚀 Just enable bootstrap - template handles everything!
  bootstrap:
    enabled: true
    serverName: "EMS-DEV"
    authEnabled: false
    allowDynamicDestinations: true
    # All other settings use smart defaults
  
  service:
    type: ClusterIP
    tcpPort: 7222
    sslPort: 7243
    httpPort: 7080
  
  persistence:
    dataSize: ""  # emptyDir for local dev
    logsSize: ""
  
  resources:
    requests: {memory: "512Mi", cpu: "250m"}
    limits: {memory: "1Gi", cpu: "500m"}
```

**What bootstrap does (built into template):**
- ✅ Creates 14 TIBCO config files (tibemsd.conf, queues.conf, acl.conf, etc.)
- ✅ Initializes 3 database files (meta.db, sync-msgs.db, async-msgs.db)
- ✅ Sets up dynamic destination support
- ✅ Configures permissions and ACLs
- ✅ **You write 0 lines of TIBCO config!** 🎉

#### **Layer 1: Configuration Files (Structured) - PRODUCTION**

**Option A: ConfigMap with Config Files**
```yaml
tibco:
  enabled: true
  bootstrap:
    enabled: false  # Disable auto-bootstrap
  
  config:
    configMapName: "tibco-prod-config"
  
  # ... rest of config ...
```

Create ConfigMap separately:
```bash
kubectl create configmap tibco-prod-config \
  --from-file=tibemsd.conf \
  --from-file=queues.conf \
  --from-file=acl.conf
```

**Option B: Init Container (Fetch from Vault)**
```yaml
tibco:
  enabled: true
  bootstrap:
    enabled: false
  
  config:
    initContainers:
    - name: fetch-tibco-config
      image: vault:latest
      command: ["/bin/sh", "-c"]
      args:
      - |
        vault kv get -field=tibemsd.conf secret/tibco > /tibco-config/tibemsd.conf
        vault kv get -field=queues.conf secret/tibco > /tibco-config/queues.conf
```

#### **Layer 2: Authentication (Secure)**
```yaml
tibco:
  enabled: true
  auth:
    username: "admin"
    password: "secure-password"
```

#### **Layer 3: Additional Environment Variables (Tuning)**
```yaml
tibco:
  enabled: true
  # ... other config ...
  env:
    EMS_LOG_LEVEL: "INFO"
    EMS_MAX_CONNECTIONS: "1000"
```

---

### 🎯 **Configuration Decision Trees**

#### **When to use what?**

| Scenario | MongoDB | TIBCO |
|----------|---------|-------|
| **Local Development** | Secret method with local secret | Bootstrap enabled (auto-config) |
| **CI/CD Pipeline** | Init container from Azure Key Vault | Bootstrap OR ConfigMap |
| **Production (Static Config)** | Secret method with prod secret | ConfigMap with prod config files |
| **Production (Dynamic Config)** | Init container from AKV/Vault | Init container from AKV/Vault |
| **Quick POC** | Secret method | Bootstrap enabled |

## 🔧 **Optional Services**

### Enable MongoDB
```yaml
mongodb:
  enabled: true
  auth:
    rootPassword: "your-root-password"
    database: "service_virtualization_prod"
    username: "sv-user"
    password: "sv-password"
```

### Enable Kafka
```yaml
kafka:
  enabled: true
  persistence:
    enabled: true
    size: 10Gi
```

### Use External TIBCO
```yaml
tibco:
  enabled: false  # Don't deploy TIBCO
  external:
    enabled: true
    host: "tibco.company.com"
    port: 7222

backend:
  env:
    TIBCO_URL: "tcp://tibco.company.com:7222"
    TIBCO_USERNAME: "prod-user"
    TIBCO_PASSWORD: "prod-password"
```

## 🌐 **Networking Configuration**

> **Important**: This chart provides **core application services only**. Clients are responsible for configuring their own networking layer based on their infrastructure requirements.

### 🚀 **Quick Access (Local Development)**

```bash
# Option 1: Port forwarding (simplest)
kubectl port-forward svc/sv-platform-ui 3000:80
kubectl port-forward svc/sv-platform-backend 8080:8080

# Access:
# UI: http://localhost:3000
# API: http://localhost:8080/api
```

### 🔧 **Production Networking Examples**

#### **Option 1: Nginx Ingress Controller**

```yaml
# ingress-nginx.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: service-virtualization-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$1
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - sv.company.com
    secretName: sv-tls-cert
  rules:
  - host: sv.company.com
    http:
      paths:
      # Backend API (must come first)
      - path: /api/?(.*)
        pathType: Prefix
        backend:
          service:
            name: sv-platform-backend
            port:
              number: 8080
      # UI (catch-all)
      - path: /(.*)
        pathType: Prefix
        backend:
          service:
            name: sv-platform-ui
            port:
              number: 80
```

#### **Option 2: Istio Service Mesh**

```yaml
# istio-gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: sv-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - sv.company.com
    tls:
      httpsRedirect: true
  - port:
      number: 443
      name: https
      protocol: HTTPS
    hosts:
    - sv.company.com
    tls:
      mode: SIMPLE
      credentialName: sv-tls-cert

---
# istio-virtualservice.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: sv-virtualservice
spec:
  hosts:
  - sv.company.com
  gateways:
  - sv-gateway
  http:
  - match:
    - uri:
        prefix: /api
    route:
    - destination:
        host: sv-platform-backend
        port:
          number: 8080
    retries:
      attempts: 3
      perTryTimeout: 10s
  - match:
    - uri:
        prefix: /
    route:
    - destination:
        host: sv-platform-ui
        port:
          number: 80
```

#### **Option 3: Multi-App Shared Domain**

For scenarios where multiple applications share a single domain:

```yaml
# Multi-app ingress with path-based routing
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: multi-app-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - app.company.com
    secretName: company-tls-cert
  rules:
  - host: app.company.com
    http:
      paths:
      # Service Virtualization
      - path: /sv/api(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: sv-platform-backend
            port:
              number: 8080
      - path: /sv(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: sv-platform-ui
            port:
              number: 80
      # Other applications...
      - path: /shop/api(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: ecommerce-backend
            port:
              number: 8080
```

#### **Option 4: Load Balancer Service**

```yaml
# loadbalancer-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: sv-platform-loadbalancer
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
    name: backend
  - port: 3000
    targetPort: 80
    name: ui
  selector:
    app.kubernetes.io/name: service-virtualization
```

### 🔐 **TLS Certificate Management**

```bash
# Option 1: cert-manager with Let's Encrypt
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Option 2: Manual certificate
kubectl create secret tls sv-tls-cert \
  --cert=sv.company.com.crt \
  --key=sv.company.com.key

# Option 3: Cloud provider certificates (AWS ACM, Azure Key Vault, etc.)
```

### 🎯 **UI Path-Aware Configuration**

The UI automatically detects deployment context:

```javascript
// Shared domain: https://app.company.com/sv/dashboard
// → API calls go to: /sv/api/*

// Dedicated domain: https://sv.company.com/dashboard  
// → API calls go to: /api/*
```

No configuration required - works automatically! 🚀

## 📊 **Resource Requirements**

### Minimum Resources
| Component | CPU | Memory | Storage |
|-----------|-----|--------|---------|
| Backend | 250m | 512Mi | - |
| UI | 50m | 64Mi | - |
| MongoDB | 100m | 256Mi | 10Gi |
| **Total** | **400m** | **832Mi** | **10Gi** |

### Production Resources
| Component | CPU | Memory | Storage |
|-----------|-----|--------|---------|
| Backend | 500m | 1Gi | - |
| UI | 100m | 128Mi | - |
| MongoDB | 500m | 1Gi | 50Gi |
| **Total** | **1100m** | **2.1Gi** | **50Gi** |

## 🔒 **Security Configuration**

### Default Security Features
- ✅ Non-root containers (UID 1000)
- ✅ Security contexts applied
- ✅ Capability dropping
- ✅ ReadOnlyRootFilesystem where possible

### Network Policies (Optional)
```yaml
networkPolicy:
  enabled: true
  ingress:
  - from: []  # Customize as needed
```

## 🛠️ **Troubleshooting**

### Common Issues

**1. Missing Environment Variables**
```bash
# Check backend logs
kubectl logs deployment/sv-platform-backend

# Look for: "Missing required environment variables"
```

**2. UI Cannot Reach Backend**
```bash
# Check ingress configuration
kubectl get ingress
kubectl describe ingress sv-platform

# Verify services
kubectl get svc
```

**3. Database Connection Issues**
```bash
# Test MongoDB connection
kubectl exec -it deployment/sv-platform-mongodb -- mongo --eval "db.adminCommand('ping')"
```

## 📚 **Advanced Examples**

### Multi-Environment Deployment
```bash
# Development
helm install sv-dev . -f values-local-dev.yaml

# Staging  
helm install sv-staging . -f values-staging.yaml --set ingress.host=staging.sv.company.com

# Production
helm install sv-prod . -f values-production.yaml --set ingress.host=sv.company.com
```

### Protocol-Specific Deployment
```bash
# Deploy with only REST and SOAP (disable messaging protocols)
helm install sv-platform . \
  --set profiles.disabled="{tibco-disabled,kafka-disabled,activemq-disabled,ibmmq-disabled}"
```

### External Services Only
```yaml
# Use all external services (no optional services deployed)
mongodb:
  enabled: false
kafka:
  enabled: false
tibco:
  enabled: false
activemq:
  enabled: false
ibmmq:
  enabled: false

backend:
  env:
    MONGODB_URI: "mongodb://external-mongo.company.com:27017"
    KAFKA_BOOTSTRAP_SERVERS: "kafka.company.com:9092"
    TIBCO_URL: "tcp://tibco.company.com:7222"
    # ... other external service configurations
```

## 📞 **Support**

For issues and questions:
- 📖 **Documentation**: See `/docs` in the repository
- 🐛 **Issues**: Create GitHub issues for bugs
- 💬 **Discussions**: Use GitHub discussions for questions

## 📄 **License**

Apache License 2.0 
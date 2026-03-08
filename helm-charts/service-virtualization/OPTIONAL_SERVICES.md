# Optional Services - Service Virtualization Platform

## 🎯 Overview

The Service Virtualization platform includes **optional messaging and database services** that can be deployed alongside the core product (Backend + UI).

Teams enable only the services they need via `values.yaml` - **all deployed with a single Helm command**.

---

## 📦 Available Optional Services

| Service | Status | Purpose | Port(s) |
|---------|--------|---------|---------|
| **IBM MQ** | ✅ Available | Message queuing | 1414, 9443 |
| **Kafka** | ✅ Available | Event streaming | 9092 |
| **TIBCO EMS** | ✅ Available | Enterprise messaging | 7222 (TCP), 7080 (HTTP) |
| **ActiveMQ** | ✅ Available | Message broker | 61616, 8161 |
| **MongoDB** | ⏸️ Skipped | Database (use dedicated instance) | - |

---

## 🚀 Single Command Deployment

```bash
# Deploy core product + enabled optional services
helm install margin-app ./helm-charts/service-virtualization \
  -f teams/margin/app-values.yaml
```

**This single command deploys:**
- ✅ Backend (core - always)
- ✅ UI (core - always)
- ✅ IBM MQ (if enabled)
- ✅ Kafka (if enabled)
- ✅ TIBCO (if enabled)
- ✅ ActiveMQ (if enabled)

---

## 📝 Configuration Examples

### **Example 1: IBM MQ Only**

```yaml
# app-values.yaml
ibmmq:
  enabled: true
  instances:
    - name: "icg"
      enabled: true
      port: 9443

kafka:
  enabled: false

tibco:
  enabled: false

activemq:
  enabled: false
```

**Result:** Deploys Backend + UI + IBM MQ

---

### **Example 2: Multiple Services**

```yaml
# app-values.yaml
ibmmq:
  enabled: true
  instances:
    - name: "icg"
      enabled: true

kafka:
  enabled: true
  zookeeper:
    enabled: true

tibco:
  enabled: true
  bootstrap:
    enabled: true

activemq:
  enabled: false
```

**Result:** Deploys Backend + UI + IBM MQ + Kafka + TIBCO

---

### **Example 3: All Services**

```yaml
# app-values.yaml
ibmmq:
  enabled: true
  instances:
    - name: "icg"
      enabled: true
    - name: "rto"
      enabled: true

kafka:
  enabled: true
  zookeeper:
    enabled: true

tibco:
  enabled: true

activemq:
  enabled: true
```

**Result:** Deploys Backend + UI + IBM MQ (2 instances) + Kafka + TIBCO + ActiveMQ

---

## 📊 Service Details

### **1. IBM MQ**

**Features:**
- ✅ Multi-instance support
- ✅ Custom MQSC configuration
- ✅ Persistent volumes
- ✅ Authentication

**Configuration:**
```yaml
ibmmq:
  enabled: true
  instances:
    - name: "icg"
      enabled: true
      port: 9443
      config:
        default:
          enabled: true  # Use default config
      persistence:
        size: "5Gi"
```

**Access:**
```bash
# Service name: {release}-mq-{instance}
kubectl get svc margin-app-mq-icg -n margin
```

---

### **2. Kafka**

**Features:**
- ✅ Event streaming
- ✅ Includes Zookeeper
- ✅ Persistent volumes
- ✅ Configurable resources

**Configuration:**
```yaml
kafka:
  enabled: true
  image:
    repository: "confluentinc/cp-kafka"
    tag: "7.4.0"
  
  replicaCount: 1
  
  persistence:
    size: "10Gi"
  
  zookeeper:
    enabled: true
    image:
      repository: "confluentinc/cp-zookeeper"
      tag: "7.4.0"
    persistence:
      dataSize: "2Gi"
      logsSize: "1Gi"
```

**Access:**
```bash
# Service name: {release}-kafka
kubectl get svc margin-app-kafka -n margin
```

---

### **3. TIBCO EMS**

**Features:**
- ✅ Enterprise messaging
- ✅ Bootstrap mode (auto-config)
- ✅ Custom config support
- ✅ TCP + HTTP ports (simplified, no SSL)

**Configuration:**
```yaml
tibco:
  enabled: true
  image:
    repository: "kytay/tibco-ems"
    tag: "latest"
  
  # Bootstrap mode (quick start)
  bootstrap:
    enabled: true
    serverName: "EMS-SERVER"
    authEnabled: false
  
  # Or custom config (production)
  config:
    configMapName: "tibco-config"  # Your ConfigMap
  
  service:
    type: ClusterIP
    tcpPort: 7222  # EMS protocol
    httpPort: 7080  # Admin/monitoring
```

**Access:**
```bash
# Service name: {release}-tibco
kubectl get svc margin-app-tibco -n margin
```

---

### **4. ActiveMQ**

**Features:**
- ✅ Message broker
- ✅ Admin UI
- ✅ Persistent volumes
- ✅ Authentication

**Configuration:**
```yaml
activemq:
  enabled: true
  image:
    repository: "rmohr/activemq"
    tag: "5.15.9"
  
  auth:
    username: "admin"
    password: "admin"
  
  service:
    type: ClusterIP
    port: 61616  # OpenWire protocol
    # Admin UI on port 8161
  
  persistence:
    dataSize: "5Gi"
    confSize: "1Gi"
```

**Access:**
```bash
# Service name: {release}-activemq
kubectl get svc margin-app-activemq -n margin

# Admin UI
kubectl port-forward svc/margin-app-activemq 8161:8161 -n margin
# Open: http://localhost:8161/admin
```

---

## 🧪 Testing

### **Deploy with Multiple Services**

```bash
# Deploy margin team with IBM MQ + Kafka
helm install margin-app ./helm-charts/service-virtualization \
  -f teams/margin/app-values.yaml \
  -n margin --create-namespace

# Verify all services
kubectl get all -n margin

# Expected output:
# pod/margin-app-backend-xxx
# pod/margin-app-ui-xxx
# pod/margin-app-mq-icg-xxx        ← IBM MQ
# pod/margin-app-kafka-xxx         ← Kafka
# pod/margin-app-zookeeper-xxx     ← Zookeeper (for Kafka)
#
# service/margin-app-backend
# service/margin-app-ui
# service/margin-app-mq-icg
# service/margin-app-kafka
# service/margin-app-zookeeper
```

---

## 🔧 Service Management

### **Enable a Service**

```yaml
# Edit app-values.yaml
tibco:
  enabled: true  # Change from false to true
```

```bash
# Upgrade release
helm upgrade margin-app ./helm-charts/service-virtualization \
  -f teams/margin/app-values.yaml
```

### **Disable a Service**

```yaml
# Edit app-values.yaml
activemq:
  enabled: false  # Change from true to false
```

```bash
# Upgrade release (removes ActiveMQ resources)
helm upgrade margin-app ./helm-charts/service-virtualization \
  -f teams/margin/app-values.yaml
```

---

## 📋 Service Templates

All templates are in: `helm-charts/service-virtualization/templates/optional-services/`

```
optional-services/
├── ibmmq.yaml      ← Multi-instance support
├── kafka.yaml      ← Includes Zookeeper
├── tibco.yaml      ← Bootstrap + custom config
└── activemq.yaml   ← Basic message broker
```

Each template:
- ✅ Conditional: `{{- if .Values.<service>.enabled }}`
- ✅ Creates: Deployment + Service + PVC (if persistence enabled)
- ✅ Configurable via values.yaml
- ✅ Production-ready with health checks
- ✅ Resource limits and requests

---

## 🎯 Architecture Benefits

### **Single Helm Release**
```bash
# One command deploys everything
helm install team-app ./helm-charts/service-virtualization -f values.yaml

# Not multiple commands like:
# helm install ibmmq ...
# helm install kafka ...
# helm install tibco ...
```

### **Unified Management**
```bash
# Single upgrade command
helm upgrade team-app ./helm-charts/service-virtualization -f values.yaml

# Single uninstall
helm uninstall team-app
```

### **Consistent Naming**
```
All services follow pattern: {release}-{service}-{instance}
- margin-app-backend
- margin-app-ui
- margin-app-mq-icg
- margin-app-kafka
- margin-app-tibco
- margin-app-activemq
```

---

## ✅ Summary

**Product Chart (service-virtualization):**
- ✅ Core product (Backend + UI) - always deployed
- ✅ Optional services (IBM MQ, Kafka, TIBCO, ActiveMQ) - conditional
- ✅ Single Helm command deploys everything
- ✅ Infrastructure-agnostic (no networking)
- ✅ Teams control via values.yaml

**Deployment:**
```bash
# One command, multiple services
helm install margin-app ./helm-charts/service-virtualization \
  -f teams/margin/app-values.yaml

# Result: Backend + UI + IBM MQ + Kafka (as configured)
```

**Flexibility:**
- 🎛️ Enable/disable per service
- 🎛️ Configure per team needs
- 🎛️ Add networking separately (Istio, Ingress, or none)

This is true **optional services** architecture - teams pick what they need! 🚀

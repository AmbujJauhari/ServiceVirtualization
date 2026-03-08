# Modular VirtualServices for HTTP Routes

## Overview

VirtualServices are organized into modular files to support flexible deployment of HTTP-based services via Istio Gateway. Each service gets its own VirtualService for independent routing configuration.

## Architecture

```
External Request → Istio Gateway (shared-gateway) → VirtualService → Service → Pod
                       (HTTP: 80/443)                 (routing)     (K8s)   (app)
```

## HTTP vs TCP Services

### **HTTP Services (use VirtualServices)** ✅
- Backend API
- UI
- IBM MQ Web Consoles (9443, 9444)
- ActiveMQ Admin Consoles (8161, 8162)
- Kafka UI Console (8080)

### **TCP Services (use LoadBalancer/NodePort directly)** 
- IBM MQ Messaging (1414) - No VirtualService needed
- Kafka (9092, 9093) - No VirtualService needed
- TIBCO EMS (7222, 7322) - No VirtualService needed
- ActiveMQ Messaging (61616, 61617) - No VirtualService needed

**TCP services are exposed via LoadBalancer/NodePort configured in Helm values, NOT via Istio Gateway!**

---

## VirtualService Structure

```
teams/
├── collateral/
│   └── networking/
│       └── istio/
│           ├── backend-vs.yaml                    # Backend API
│           ├── ui-vs.yaml                         # UI
│           ├── ibmmq-icg-console-vs.yaml          # IBM MQ ICG console
│           ├── ibmmq-rto-console-vs.yaml          # IBM MQ RTO console
│           ├── activemq-broker1-console-vs.yaml   # ActiveMQ broker1 console
│           ├── activemq-broker2-console-vs.yaml   # ActiveMQ broker2 console
│           └── kafka-ui-vs.yaml                   # Kafka UI console
│
└── margin/
    └── networking/
        └── istio/
            ├── backend-vs.yaml
            ├── ui-vs.yaml
            ├── ibmmq-icg-console-vs.yaml
            ├── ibmmq-rto-console-vs.yaml
            ├── activemq-broker1-console-vs.yaml
            ├── activemq-broker2-console-vs.yaml
            └── kafka-ui-vs.yaml
```

---

## URL Path Structure

### **Collateral Team**

| Service | External URL | Internal Service | Port |
|---------|--------------|------------------|------|
| Backend API | `http://gateway:30080/collateral/api/*` | `backend:8080` | 8080 |
| UI | `http://gateway:30080/collateral/*` | `ui:80` | 80 |
| IBM MQ ICG Console | `http://gateway:30080/collateral/mq-icg/*` | `mq-icg:9443` | 9443 |
| IBM MQ RTO Console | `http://gateway:30080/collateral/mq-rto/*` | `mq-rto:9444` | 9444 |
| ActiveMQ Broker1 | `http://gateway:30080/collateral/activemq/broker1/*` | `activemq-broker1:8161` | 8161 |
| ActiveMQ Broker2 | `http://gateway:30080/collateral/activemq/broker2/*` | `activemq-broker2:8162` | 8162 |
| Kafka UI | `http://gateway:30080/collateral/kafka-ui/*` | `kafka-ui:8080` | 8080 |

### **Margin Team**

| Service | External URL | Internal Service | Port |
|---------|--------------|------------------|------|
| Backend API | `http://gateway:30080/margin/api/*` | `backend:8080` | 8080 |
| UI | `http://gateway:30080/margin/*` | `ui:80` | 80 |
| IBM MQ ICG Console | `http://gateway:30080/margin/mq-icg/*` | `mq-icg:9443` | 9443 |
| IBM MQ RTO Console | `http://gateway:30080/margin/mq-rto/*` | `mq-rto:9444` | 9444 |
| ActiveMQ Broker1 | `http://gateway:30080/margin/activemq/broker1/*` | `activemq-broker1:8161` | 8161 |
| ActiveMQ Broker2 | `http://gateway:30080/margin/activemq/broker2/*` | `activemq-broker2:8162` | 8162 |
| Kafka UI | `http://gateway:30080/margin/kafka-ui/*` | `kafka-ui:8080` | 8080 |

**Team isolation via path prefix** - `/collateral/*` vs `/margin/*`

---

## Deployment Patterns

### **Deploy Single Service VirtualService**

```bash
# Deploy only backend VirtualService
kubectl apply -f teams/margin/networking/istio/backend-vs.yaml
```

### **Deploy Multiple Service VirtualServices**

```bash
# Deploy backend + IBM MQ console VirtualServices
kubectl apply -f teams/margin/networking/istio/backend-vs.yaml
kubectl apply -f teams/margin/networking/istio/ibmmq-icg-console-vs.yaml
```

### **Deploy All VirtualServices for a Team**

```bash
# Deploy all HTTP routes for margin team
kubectl apply -f teams/margin/networking/istio/
```

### **Deploy Specific Consoles Only**

```bash
# Deploy only consoles (no backend/UI)
kubectl apply -f teams/collateral/networking/istio/ibmmq-icg-console-vs.yaml
kubectl apply -f teams/collateral/networking/istio/activemq-broker1-console-vs.yaml
kubectl apply -f teams/collateral/networking/istio/kafka-ui-vs.yaml
```

---

## Path Rewriting

All VirtualServices use path rewriting:

```yaml
http:
  - match:
    - uri:
        prefix: "/margin/mq-icg/"    # External path
    rewrite:
      uri: "/"                        # Internal path (rewritten)
    route:
    - destination:
        host: margin-app-mq-icg.margin.svc.cluster.local
```

**Example:**
- External: `http://gateway:30080/margin/mq-icg/console/login`
- Rewritten: `http://margin-app-mq-icg:9443/console/login`

**Why:** Services expect requests at root (`/`), but teams need path-based isolation (`/margin/*` vs `/collateral/*`)

---

## Gateway Configuration

All VirtualServices reference the shared Istio Gateway:

```yaml
spec:
  gateways:
  - istio-ingress/shared-gateway    # Shared gateway for all teams
```

The shared gateway is deployed by Terraform and listens on:
- Port 80 (HTTP) → NodePort 30080
- Port 443 (HTTPS) → NodePort 30443

---

## Prerequisites

1. **Istio installed** (via Terraform)
2. **Shared Gateway deployed** (`istio-ingress/shared-gateway`)
3. **Services deployed** (via Helm with team values files)

**Deploy order:**
1. Terraform: Install Istio + create namespaces
2. Helm: Deploy services (backend, UI, IBM MQ, etc.)
3. Kubectl: Deploy VirtualServices

---

## Verification

### **Check VirtualServices**

```bash
# List all VirtualServices in margin namespace
kubectl get virtualservices -n margin

# Describe specific VirtualService
kubectl describe virtualservice margin-backend-vs -n margin
```

### **Test HTTP Routes**

```bash
# Test backend API
curl http://192.168.56.10:30080/margin/api/health

# Test UI
curl http://192.168.56.10:30080/margin/

# Test IBM MQ console
curl http://192.168.56.10:30080/margin/mq-icg/

# Test Kafka UI
curl http://192.168.56.10:30080/margin/kafka-ui/
```

---

## Kafka UI Integration

Kafka UI is deployed as part of the Kafka service and auto-configured to connect to all Kafka instances.

**Configuration in `kafka-values.yaml`:**
```yaml
kafka:
  enabled: true
  
  ui:
    enabled: true              # Enable Kafka UI console
    replicaCount: 1
    image:
      repository: "provectuslabs/kafka-ui"
      tag: "latest"
    service:
      type: "ClusterIP"
      port: 8080
```

**Kafka UI automatically discovers all Kafka instances** defined in `kafka.instances`.

---

## Benefits

✅ **Modular**: Deploy only the VirtualServices you need
✅ **Team Isolation**: Path-based routing (`/margin/*` vs `/collateral/*`)
✅ **Independent**: Each service has its own routing config
✅ **Flexible**: Add/remove services without affecting others
✅ **Consistent**: Same pattern across all HTTP services

---

## Notes

- **VirtualServices are for HTTP only** - TCP services use LoadBalancer/NodePort
- **One VirtualService per HTTP service** - Easy to manage and debug
- **Path rewriting** - External paths (`/margin/api/*`) → Internal paths (`/api/*`)
- **Shared Gateway** - All teams use `istio-ingress/shared-gateway`
- **Deploy after Helm** - VirtualServices need services to exist first

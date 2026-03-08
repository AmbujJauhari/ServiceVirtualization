# Service Virtualization Platform

Service Virtualization is a Kubernetes-native platform that runs lightweight simulations of enterprise messaging services — IBM MQ, Tibco EMS, Apache Kafka, and ActiveMQ — alongside REST and SOAP stub servers. It is designed for non-production environments where connecting to real downstream services is impractical, unavailable, or costly.

The platform is deployed per team. Each team gets their own isolated namespace with independent instances of whichever services they need. A single shared Istio ingress gateway routes all external traffic — HTTP, HTTPS, and raw TCP — using path-based and SNI-based rules.

---

## Table of Contents

- [Architecture](#architecture)
- [Supported Protocols](#supported-protocols)
- [Optional Services](#optional-services)
  - [IBM MQ](#ibm-mq)
  - [Tibco EMS](#tibco-ems)
  - [Apache Kafka](#apache-kafka)
  - [ActiveMQ](#activemq)
  - [MongoDB](#mongodb)
- [Core Product](#core-product)
- [Documentation](#documentation)

---

## Architecture

The platform is split into three independent layers, each owned by a different persona.

```
┌─────────────────────────────────────────────────────────────────┐
│  Layer 1 — Cluster Admin                                        │
│  deployments/local/cluster-admin/                               │
│                                                                 │
│  Provisions the K3s cluster (Vagrant), installs Istio, creates  │
│  team namespaces and RBAC roles, and deploys the shared         │
│  Gateway resource. Done once per environment.                   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│  Layer 2 — Product (Helm Chart)                                 │
│  helm-charts/service-virtualization/                            │
│                                                                 │
│  Infrastructure-agnostic Helm chart. Renders Deployments and    │
│  Services for the backend, UI, and all optional messaging       │
│  services. Has no opinion on Ingress, Istio, or DNS.            │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│  Layer 3 — Team Deployment                                      │
│  deployments/local/teams/teams/<team>/                          │
│                                                                 │
│  Per-team values files, Istio VirtualServices, and RBAC charts. │
│  Each team deploys and manages their own namespace.             │
└─────────────────────────────────────────────────────────────────┘
```

### Gateway port design

| Gateway port | Protocol | Routing type | Used for |
|---|---|---|---|
| 80 | HTTP | Path-based VirtualService | UI, Backend API |
| 443 | TLS PASSTHROUGH | SNI-based VirtualService | IBM MQ wire protocol, Tibco EMS SSL, Kafka SSL |
| 8443 | HTTPS termination | Path-based VirtualService | IBM MQ web console |

The TLS PASSTHROUGH port (443) does **not** terminate TLS at the gateway — it reads the SNI hostname from the `ClientHello` and routes to the correct pod, where the service itself terminates TLS. This means each service instance must hold a valid TLS certificate.

The HTTPS port (8443) terminates TLS at the gateway and speaks plain HTTP to the upstream service, enabling standard path-based routing for web consoles.

### Naming conventions

All resource names are deterministic from the Helm release name and instance name.

| Service | Kubernetes Service name | Pattern |
|---|---|---|
| IBM MQ | `margin-ibmmq-mq-icg` | `<release>-mq-<instance>` |
| Tibco EMS | `margin-app-service-virtualization-tibco-servera` | `<fullname>-tibco-<instance>` |
| Kafka | `margin-app-service-virtualization-kafka-events` | `<fullname>-kafka-<instance>` |
| ActiveMQ | `margin-app-service-virtualization-activemq-broker1` | `<fullname>-activemq-<instance>` |
| MongoDB | `margin-mongodb` | `mongodb.nameOverride` (recommended) or `<release>-mongodb` |
| Backend | `margin-app-service-virtualization-backend` | `<fullname>-backend` |
| UI | `margin-app-service-virtualization-ui` | `<fullname>-ui` |

`<fullname>` expands to `<release>-service-virtualization` from `_helpers.tpl`.

### TLS certificate strategy

A single wildcard certificate (`*.service-virtualization.local`) is shared across all services in an environment. It is stored as a Kubernetes Secret and referenced by:

- The Istio Gateway (`gateway-tls-cert` in `istio-ingress` namespace) — for HTTPS termination
- Each team's TLS secret (`margin-tls-secret`, `collateral-tls-secret`) — mounted into service pods for their own TLS listeners and used by the backend init container to build a JKS truststore

---

## Supported Protocols

| Protocol | Image | Ports | TLS support |
|---|---|---|---|
| IBM MQ | `icr.io/ibm-mqadvanced-server-dev` | 1414 (wire), 9443 (console) | Yes — TLS PASSTHROUGH via Istio |
| Tibco EMS | `tibco/ems` | 7222 (TCP), 7243 (SSL) | Yes — TLS PASSTHROUGH via Istio |
| Apache Kafka | `confluentinc/cp-kafka` | 9092 (PLAINTEXT), 9094 (SSL) | Yes — TLS PASSTHROUGH via Istio |
| ActiveMQ | `apache/activemq-artemis` | 61616 (OpenWire), 8161 (admin) | Planned |
| REST/HTTP | WireMock (embedded in backend) | 8080 | Via Istio HTTPS |
| SOAP | WireMock (embedded in backend) | 8080 | Via Istio HTTPS |

---

## Optional Services

Each optional service is enabled by setting `<service>.enabled: true` in your values file and providing at least one instance under `<service>.instances`. Every service supports multiple named instances within the same Helm release.

### IBM MQ

Deploys `ibm-mqadvanced-server-dev` with a fully generated MQSC configuration. Each instance runs a dedicated queue manager.

**Key values:**

```yaml
ibmmq:
  enabled: true
  instances:
    icg:                           # instance name — becomes part of all resource names
      replicaCount: 1
      image:
        repository: icr.io/ibm-mqadvanced-server-dev
        tag: "9.3.4.0-r1"
      config:
        default:
          enabled: true            # auto-generate MQSC (recommended for dev)
          queueManagerName: QM_ICG
          channel: DEV.APP.SVRCONN
          queues:
            - DEV.QUEUE.1
        custom:
          mqsc: ""                 # provide raw MQSC when default.enabled: false
      certificates:
        enabled: true
        initContainer: {}          # init container that writes tls.crt + tls.key to emptyDir
```

**Istio routing:** SNI-based on `<team>2e-<instance>2e-svrconn.chl.mq.ibm.com` for the wire protocol. HTTPS path-based for the web console at `https://<instance>.ibmmq.<team>.service-virtualization.local:<https-nodeport>/<team>/mq-<instance>/ibmmq/console/`.

**Files:**
- Template: `helm-charts/service-virtualization/templates/optional-services/ibmmq.yaml`
- Margin ICG values: `deployments/local/teams/teams/margin/umbrella/values/ibmmq-icg.yaml`
- Margin RTO values: `deployments/local/teams/teams/margin/umbrella/values/ibmmq-rto.yaml`
- Istio routing: `deployments/local/teams/teams/margin/umbrella/templates/ibmmq.yaml`

---

### Tibco EMS

Deploys Tibco EMS with dual listeners: plain TCP on 7222 (internal) and SSL on 7243 (external via Istio). Configuration is generated from a `tibemsd.conf` template or supplied inline.

**Key values:**

```yaml
tibco:
  enabled: true
  instances:
    servera:
      replicaCount: 1
      image:
        repository: tibco/ems
        tag: "10.2"
      config:
        default:
          enabled: true            # auto-generate tibemsd.conf
          serverName: EMS-SERVERA
          clientTimeout: 600
      certificates:
        enabled: true
        initContainer: {}          # init container that writes tls.crt + tls.key to emptyDir
```

**Istio routing:** TLS PASSTHROUGH on port 443, SNI host `<instance>.tibco.<team>.service-virtualization.local`.

**Backend connection:** The backend connects to Tibco using `tcp://<service-fqdn>:7222` (plain TCP, no SSL). Port 7243 (SSL) is reserved for external clients routing through Istio PASSTHROUGH. Using `ssl://` on port 7222 would fail because that listener only accepts plain TCP connections.

**Files:**
- Template: `helm-charts/service-virtualization/templates/optional-services/tibco.yaml`
- Margin ServerA values: `deployments/local/teams/teams/margin/umbrella/values/tibco-servera.yaml`
- Istio routing: `deployments/local/teams/teams/margin/umbrella/templates/tibco.yaml`

---

### Apache Kafka

Deploys Confluent Platform Kafka in KRaft mode (no Zookeeper). Each instance runs a combined broker+controller. Dual listeners are configured: PLAINTEXT on 9092 (internal) and SSL on 9094 (external via Istio).

**Key values:**

```yaml
kafka:
  enabled: true
  instances:
    events:
      replicaCount: 1
      image:
        repository: confluentinc/cp-kafka
        tag: "7.4.0"
      clusterID: "MkU3OEVBNTcwNTJENDM2Qk"   # unique per instance, generate with kafka-storage
      externalHostname: "events.kafka.margin.service-virtualization.local"
      certificates:
        enabled: true
        secretName: margin-tls-secret
```

**Istio routing:** TLS PASSTHROUGH on port 443, SNI host matches `externalHostname`.

**Backend connection:** The backend connects to Kafka using the **PLAINTEXT listener on port 9092** (not SSL), using the internal ClusterIP FQDN. This is intentional — Kafka's SSL listener advertises the external Istio hostname in its metadata, which is not resolvable by pods inside the cluster. PLAINTEXT on 9092 advertises the internal FQDN and works correctly for in-cluster connections.

**Files:**
- Template: `helm-charts/service-virtualization/templates/optional-services/kafka.yaml`
- Margin events values: `deployments/local/teams/teams/margin/umbrella/values/kafka-events.yaml`
- Margin analytics values: `deployments/local/teams/teams/margin/umbrella/values/kafka-analytics.yaml`
- Istio routing: `deployments/local/teams/teams/margin/umbrella/templates/kafka.yaml`

---

### ActiveMQ

Deploys Apache ActiveMQ Artemis with the OpenWire protocol on port 61616 and the admin console on port 8161.

**Key values:**

```yaml
activemq:
  enabled: true
  instances:
    broker1:
      replicaCount: 1
      image:
        repository: apache/activemq-artemis
        tag: "2.31.2"
      service:
        port: 61616
        adminPort: 8161
```

**Istio routing:** HTTP path-based VirtualService for the admin console. OpenWire clients connect directly to the ClusterIP service from within the cluster or via a NodePort.

**Files:**
- Template: `helm-charts/service-virtualization/templates/optional-services/activemq.yaml`
- Margin values: `deployments/local/teams/teams/margin/umbrella/values/activemq.yaml`
- Istio routing: `deployments/local/teams/teams/margin/umbrella/templates/activemq.yaml`

---

### MongoDB

Deploys a MongoDB instance as the backing store for the Service Virtualization backend. This is an **optional** service — teams that already run their own MongoDB, Azure Cosmos DB, or use Sybase/MSSQL should leave it disabled and point `MONGODB_URI` at their existing instance.

**Key values:**

```yaml
mongodb:
  enabled: true
  nameOverride: "margin-mongodb"   # fixes the service name; MUST match MONGODB_URI in backend-values.yaml
  image:
    repository: mongo
    tag: "7.0"
  auth:
    enabled: false                 # disabled for local dev; enable with rootUsername/rootPassword for prod
  storage:
    enabled: false                 # emptyDir by default; set enabled: true + size/storageClass for PVC
```

**Naming:** The `nameOverride` field is critical. It pins the Kubernetes Service name to a fixed value (e.g., `margin-mongodb`) regardless of which Helm release deploys the chart. The `MONGODB_URI` in `backend-values.yaml` must match:

```
MONGODB_URI: "mongodb://margin-mongodb.margin.svc.cluster.local:27017"
```

Without `nameOverride` the service name would be `<release>-mongodb`, which changes if the release is renamed.

**Bring your own database:** Teams that do not want a MongoDB pod should set `mongodb.enabled: false` and configure the backend to use their own instance:

```yaml
# backend-values.yaml
env:
  MONGODB_URI: "mongodb://your-external-host:27017"
  # For Sybase/MSSQL — configure JDBC datasource properties instead and
  # remove SPRING_AUTOCONFIGURE_EXCLUDE for DataSourceAutoConfiguration.
```

**Files:**
- Template: `helm-charts/service-virtualization/templates/optional-services/mongodb.yaml`
- Margin values: `deployments/local/teams/teams/margin/umbrella/values/mongodb.yaml`
- Collateral values: `deployments/local/teams/teams/collateral/umbrella/values/mongodb.yaml`

---

## Core Product

### Backend

Spring Boot application (Java 21) that manages stubs and handles incoming messages across all configured protocols. It exposes a REST API consumed by the UI.

The backend uses a registry-based configuration model: each messaging service instance is registered via environment variables (`IBMMQ_REGISTRY_<NAME>_*`, `TIBCO_REGISTRY_<NAME>_*`, `KAFKA_REGISTRY_<NAME>_*`). Multiple instances of each protocol can be registered independently.

**TLS configuration in the backend pod:**

An init container (`truststore-creator`) runs at pod startup and converts `tls.crt` from the team's TLS secret into a JKS truststore at `/app/certs/truststore.jks`. The main container is then started with:

```
JAVA_OPTS: >-
  -Djavax.net.ssl.trustStore=/app/certs/truststore.jks
  -Djavax.net.ssl.trustStorePassword=changeit
  -Dcom.ibm.mq.cfg.preferTLS=true
```

**Istio routing:** HTTP path-based on `/margin/api/*` → rewrites to `/api/*` on the backend pod.

**Files:**
- Template: `helm-charts/service-virtualization/templates/core-product/backend.yaml`
- Margin values: `deployments/local/teams/teams/margin/umbrella/values/backend.yaml`
- Istio routing: `deployments/local/teams/teams/margin/umbrella/templates/backend-ui.yaml`

### UI

React 18 / TypeScript single-page application served by nginx. Communicates with the backend API.

At container startup, nginx substitutes the `BACKEND_URL` environment variable into its configuration using `envsubst`. In production (Istio), the browser sends API calls to the full prefixed path (e.g., `/margin/api/*`) which Istio routes directly to the backend — nginx proxy_pass is not involved. The `BACKEND_URL` value is a fallback for local Docker testing without Istio.

`configLoader.ts` auto-detects the URL prefix (`/margin/`, `/collateral/`, `/sv/`, or root) from `window.location.pathname` and sets the API base URL accordingly. `BrowserRouter` is initialized with the same prefix as `basename` so React Router generates correct hrefs.

**Docker build:**

```bash
# Multi-stage build: node:20-alpine compiles the app, nginx:1.25-alpine serves it
npm run docker:build
# or
docker build -t service-virtualization-ui:latest UI/
```

**Istio routing:** HTTP path-based on `/margin/*` → rewrites to `/` on the UI nginx pod.

**Files:**
- Template: `helm-charts/service-virtualization/templates/core-product/ui.yaml`
- Margin values: `deployments/local/teams/teams/margin/umbrella/values/ui.yaml`
- Istio routing: `deployments/local/teams/teams/margin/umbrella/templates/backend-ui.yaml`

---

## Documentation

| Document | Purpose |
|---|---|
| [LOCAL_DEPLOYMENT_GUIDE.md](LOCAL_DEPLOYMENT_GUIDE.md) | Step-by-step guide to deploy the full platform on a local K3s cluster using Vagrant |
| [AKS_DEPLOYMENT_GUIDE.md](AKS_DEPLOYMENT_GUIDE.md) | Deploying to Azure Kubernetes Service and private clusters |
| [TROUBLESHOOTING_GUIDE.md](TROUBLESHOOTING_GUIDE.md) | Debugging common issues across all protocols and infrastructure layers |

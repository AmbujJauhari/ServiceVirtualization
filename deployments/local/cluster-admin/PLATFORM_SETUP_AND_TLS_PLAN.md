# Platform Setup & TLS Roadmap
## Service Virtualization on K3s (Local) → AKS (Production)

> **Purpose:** Reference document covering the cluster-admin setup, Istio architecture,
> multi-tenant routing model, and the full TLS compliance plan for all optional services.

---

## Table of Contents

1. [Infrastructure Overview](#1-infrastructure-overview)
2. [Cluster Admin Responsibilities](#2-cluster-admin-responsibilities)
3. [Step-by-Step: Installing Istio](#3-step-by-step-installing-istio)
4. [Traffic Routing Architecture](#4-traffic-routing-architecture)
5. [Multi-Tenant Model (Margin & Collateral)](#5-multi-tenant-model-margin--collateral)
6. [AKS Production Considerations](#6-aks-production-considerations)
7. [TLS Compliance Plan](#7-tls-compliance-plan)
8. [Core Product TLS Client Support](#8-core-product-tls-client-support)
9. [Helm Chart TLS Gaps — Findings](#9-helm-chart-tls-gaps--findings)
10. [Bring Your Own Domain (BYOD)](#10-bring-your-own-domain-byod)

---

## 1. Infrastructure Overview

### Local Development (K3s via Vagrant/VirtualBox)

```
Host Machine (Windows)
  │
  │  VirtualBox Bridge Network
  │  Host accesses VM at: 10.0.2.15
  │
  └─► K3s VM
        ├── kubeconfig-local.yaml   → server: https://127.0.0.1:6443  (use inside VM)
        └── kubeconfig-bridge.yaml  → server: https://10.0.2.15:6443  (use from host)
```

### Two Kubeconfigs — When to Use Each

| File | Server | Use When |
|---|---|---|
| `kubeconfig-local.yaml` | `127.0.0.1:6443` | Running `terraform` / `kubectl` **inside the VM** |
| `kubeconfig-bridge.yaml` | `10.0.2.15:6443` | Running `terraform` / `kubectl` **from host Windows machine** |

Both files have identical credentials (same cert/key). The only difference is the server address.

---

## 2. Cluster Admin Responsibilities

The `cluster-admin` role (this directory) owns:

| Resource | How Created | Purpose |
|---|---|---|
| Team namespaces (`margin`, `collateral`) | Terraform | Isolated deployment space per team |
| `namespace-admin` Role | Terraform | Full permissions within a namespace |
| Istio installation (`istio-system`) | Terraform + Helm | Service mesh control plane |
| Istio Ingress Gateway (`istio-ingress`) | Terraform + Helm | Single entry point for all external traffic |
| Shared Gateway resource | Terraform (`gateway.tf`) | Defines what ports/hosts the gateway accepts |
| TLS wildcard certificate Secret | Manual or cert-manager | Enables SNI routing on port 443 |

Teams are responsible for:
- Their own Deployments, Services, ConfigMaps, Secrets (via their Helm chart)
- Their own VirtualServices (referencing `istio-ingress/shared-gateway`)
- Their own ServiceAccounts (created by Helm, bound to cluster-admin's Role)

---

## 3. Step-by-Step: Installing Istio

### Prerequisites

```bash
# From inside the VM (or use kubeconfig-bridge from host)
terraform --version   # >= 1.0
kubectl version
helm version
```

### Step 1 — Configure Variables

Edit `terraform/istio/terraform.tfvars`:

```hcl
# Which kubeconfig to use
kubeconfig_path    = "../../kubeconfig-local.yaml"   # or kubeconfig-bridge.yaml from host
kubeconfig_context = "k3s-local"                      # or k3s-bridge

# Team namespaces to create
team_namespaces = ["margin", "collateral"]
environment     = "dev"

# Istio version
istio_version = "1.20.0"

# Gateway
install_ingress_gateway = true
gateway_name            = "shared-gateway"
gateway_namespace       = "istio-ingress"
gateway_hosts           = ["*"]
```

### Step 2 — Initialize Terraform

```bash
cd deployments/local/cluster-admin/terraform/istio
terraform init
```

> **Note:** Helm provider v3.x is required (pinned in `main.tf`). This version changed
> the `kubernetes {}` block syntax to `kubernetes = {}` and `set {}` blocks to `set = [{}]`.

### Step 3 — Plan and Apply

```bash
terraform plan
terraform apply
```

### What Gets Created (in order)

```
Step 0:  Team namespaces (margin, collateral)
Step 0.1: namespace-admin RBAC Roles (one per namespace)
Step 1:  istio-base Helm release     → Installs Istio CRDs
Step 2:  istiod Helm release          → Istio control plane
Step 3:  istio-ingressgateway         → Envoy proxy pod (NodePort on K3s)
Step 4:  shared-gateway resource      → Tells ingressgateway what to accept
```

### Step 4 — Verify

```bash
# Check namespaces
kubectl get namespaces -l app.kubernetes.io/part-of=service-virtualization

# Check Istio pods
kubectl get pods -n istio-system
kubectl get pods -n istio-ingress

# Check the shared gateway
kubectl get gateway.networking.istio.io -n istio-ingress

# Get NodePort for HTTP access (K3s local)
kubectl get svc -n istio-ingress
# Look for port 80:3xxxx/TCP — the 3xxxx is your NodePort
```

---

## 4. Traffic Routing Architecture

### Component Roles

```
[Client]
   │
   │  HTTP: http://10.0.2.15:<NodePort>/path
   │  TCP:  kafka-host:<NodePort>
   ▼
NodePort (K3s)        ← "hole in the VM wall" — only way in for local K3s
   │
   ▼
istio-ingressgateway Pod (Envoy proxy)
   │  Reads: Gateway resource  → "what ports/hosts am I listening on?"
   │  Reads: VirtualService    → "where does this request go?"
   ▼
Kubernetes Service (ClusterIP)
   │
   ▼
Application Pod
```

### HTTP Routing (L7 — by path/host header)

```
One NodePort handles unlimited teams and services:
  GET /margin/api/stubs  →  margin-svc
  GET /collateral/api/stubs  →  collateral-svc
  GET /margin/kafka-ui       →  margin-kafka-ui-svc
```

### TCP Routing (L4 — by port number, no TLS)

```
One NodePort per TCP service:
  NodePort 31092  →  margin Kafka
  NodePort 31093  →  collateral Kafka
  NodePort 31414  →  margin IBM MQ
  (port number IS the routing key — no other metadata available)
```

### TCP Routing (L4 — by SNI hostname, with TLS)

```
One port (443) handles all services via hostname:
  SNI=kafka.margin.company.com    →  margin Kafka
  SNI=kafka.collateral.company.com  →  collateral Kafka
  SNI=ibmmq.margin.company.com    →  margin IBM MQ
  (hostname in TLS handshake IS the routing key — port explosion eliminated)
```

---

## 5. Multi-Tenant Model (Margin & Collateral)

### Isolation

Each team has a dedicated namespace. RBAC ensures teams cannot access each other's resources:

```
margin namespace:
  └── namespace-admin Role  (full permissions within margin only)
      └── margin-sa ServiceAccount  (created by team's Helm chart)

collateral namespace:
  └── namespace-admin Role  (full permissions within collateral only)
      └── collateral-sa ServiceAccount  (created by team's Helm chart)
```

### Shared vs. Owned Resources

```
SHARED (cluster-admin owns, teams reference):
  istio-ingress/shared-gateway    ← teams put this in their VirtualService.gateways

OWNED BY EACH TEAM (deployed via their Helm chart):
  Deployments, Services, VirtualServices, ConfigMaps, Secrets
```

### VirtualService Pattern (each team deploys)

```yaml
# margin team's kafka VirtualService (deployed via their Helm chart)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: margin-kafka-vs
  namespace: margin
spec:
  hosts: ["kafka.margin.service-virt.company.com"]
  gateways: ["istio-ingress/shared-gateway"]          # references cluster-admin's gateway
  tls:
  - match:
    - port: 443
      sniHosts: ["kafka.margin.service-virt.company.com"]
    route:
    - destination:
        host: <release>-kafka-primary
        port: { number: 9092 }
```

---

## 6. AKS Production Considerations

### What Changes vs. Local K3s

| Aspect | Local K3s | AKS Production |
|---|---|---|
| Service type | `NodePort` | `LoadBalancer` |
| Entry IP | VM IP + NodePort | Azure Load Balancer public/private IP |
| DNS | Manual hosts file | Real DNS / GSLB |
| TLS certs | Self-signed / embedded | Corporate CA or Let's Encrypt via cert-manager |
| Port count (HTTP) | 1 NodePort | 1 Azure LB rule |
| Port count (TCP, no TLS) | 1 NodePort per service | 1 Azure LB rule + 1 NSG rule per service |
| Port count (TCP, with TLS+SNI) | 1 NodePort for all | 1 Azure LB rule + 1 NSG rule for all |

### GSLB Integration

- GSLB is DNS-based — it returns an IP address, not a port
- For HTTP: GSLB → Azure LB IP → port 443 → Istio routes by `Host:` header. Clean.
- For TCP without TLS: GSLB provides the IP; client must know the specific port. Port is not GSLB-routable.
- For TCP with TLS: GSLB provides the IP; Istio routes by SNI hostname. **One port handles all.**

### Azure Port Requirements

```
With TLS + SNI (recommended):
  Azure NSG:  allow inbound 443
  Azure LB:   one rule, port 443
  Result:     all teams, all protocols, all services → 1 port

Without TLS (current local state):
  Azure NSG:  one rule per TCP service port
  Azure LB:   one rule per TCP service port
  Result:     N teams × M protocols = N×M ports to manage
```

---

## 7. TLS Compliance Plan

### Phase 1 — Istio Gateway TLS Support (cluster-admin, Terraform)

The shared gateway currently only has HTTP (port 80). Add:

- [ ] Port 443 server entry with TLS mode `SIMPLE` using a wildcard cert Secret
- [ ] TCP/TLS server entries for SNI passthrough (if services handle their own TLS)
- [ ] Wildcard TLS certificate Secret in `istio-ingress` namespace
- [ ] Consider cert-manager for automated certificate rotation

### Phase 2 — Helm Chart VirtualServices (per optional service)

All four optional service templates (Kafka, IBM MQ, TIBCO, ActiveMQ) are **missing VirtualService resources entirely**. Each service needs a VirtualService added to its template that:

- [ ] References `istio-ingress/shared-gateway`
- [ ] Specifies the SNI hostname pattern (templated from values)
- [ ] Routes to the correct backend Service

### Phase 3 — Certificate Format Fixes (per service)

| Service | Problem | Fix Required |
|---|---|---|
| **Kafka** | Requires JKS keystore; cert helpers produce PEM | Add `keytool` init container to convert PEM → JKS |
| **IBM MQ** | `MQ_SSLKEYR` expects KDB format; cert helpers produce PEM | Switch to PEM-based env vars (MQ 9.2+) OR add `runmqckm` init container |
| **TIBCO** | Uses PEM natively — already compatible | No format change needed |
| **ActiveMQ** | Requires JKS keystore; cert helpers produce PEM | Add `keytool` init container to convert PEM → JKS |

### Phase 4 — Hardcoded Secret Fixes

- [ ] **Kafka**: `KAFKA_SSL_KEYSTORE_PASSWORD` and `KAFKA_SSL_TRUSTSTORE_PASSWORD` are hardcoded as `"changeit"` — must be configurable via values (referencing a K8s Secret)
- [ ] **ActiveMQ**: `ACTIVEMQ_SSL_KEYSTORE_PASSWORD` and `ACTIVEMQ_SSL_TRUSTSTORE_PASSWORD` are hardcoded as `"changeit"` — same fix needed

### Phase 5 — Port and Listener Fixes

- [ ] **TIBCO**: `service.sslPort` is defined in `values.yaml` but the Service template always uses `tcpPort` regardless of SSL state. The Service port should switch to `sslPort` when `$instanceSslEnabled` is true.
- [ ] **ActiveMQ**: Plain TCP uses container port `61616`; SSL typically uses `61617`. The service `targetPort` is hardcoded to `61616`. Needs conditional logic.

### Phase 6 — Kafka Advertised Listeners (Critical for External Access)

Kafka performs a **second-level hostname negotiation** — after a client connects, the broker sends back its `ADVERTISED_LISTENERS` address and the client reconnects to that address. Currently set to the internal K8s service name:

```
KAFKA_ADVERTISED_LISTENERS: SSL://<release>-kafka-<name>:9092
```

For external SNI-based access, this must be the **external hostname** (e.g., `kafka.margin.service-virt.company.com`). Required changes:

- [ ] Add `advertisedHost` value per Kafka instance in `values.yaml`
- [ ] Make `KAFKA_ADVERTISED_LISTENERS` use this value when set and SSL is enabled

> **Note:** IBM MQ, TIBCO, and ActiveMQ do NOT have this problem — they do not redirect
> the client to a different address after connection.

---

## 8. Core Product TLS Client Support

> **This is required regardless of which optional services are enabled.**

When `global.certificates.enabled: true`, the optional services (Kafka, IBM MQ, TIBCO, ActiveMQ)
will present TLS on their connections. The **backend application** (the Spring Boot service
virtualization core product) connects to these services **as a client** and must be updated
to support TLS connections.

### What the Backend Needs

#### Kafka Client (Spring Kafka / KafkaProducer/Consumer)

The backend's Kafka client configuration must include SSL properties when connecting to a
TLS-enabled Kafka broker:

```properties
# application.yml (when SSL enabled)
spring.kafka.security.protocol=SSL
spring.kafka.ssl.trust-store-location=file:/path/to/truststore.jks
spring.kafka.ssl.trust-store-password=<password>
spring.kafka.ssl.key-store-location=file:/path/to/keystore.jks
spring.kafka.ssl.key-store-password=<password>
```

- [ ] The backend pod needs the truststore/keystore mounted as a volume
- [ ] `backend.certificateVolumes` in `values.yaml` must be populated with the cert Secret
- [ ] Backend env vars must switch between plain and SSL based on whether SSL is enabled

#### IBM MQ Client (IBM MQ JMS)

```properties
# Connection factory SSL properties
com.ibm.mq.cfg.channel.ssl.cipherSuite=TLS_RSA_WITH_AES_256_GCM_SHA384
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=<password>
```

- [ ] IBM MQ client requires a JKS truststore containing the MQ server's certificate
- [ ] The backend pod needs this truststore mounted

#### TIBCO EMS Client (TIBCO JMS)

TIBCO's Java client (`tibjms.jar`) uses its own SSL properties:

```java
TibjmsSSL.addTrustedCerts("/path/to/ca.crt");
TibjmsSSL.setClientIdentity("/path/to/tls.crt", "/path/to/tls.key", null);
```

Or via connection factory:
```properties
com.tibco.tibjms.ssl.trusted_certs=/tibco/certs/ca.crt
```

- [ ] Backend needs CA cert mounted (PEM format is fine for TIBCO)
- [ ] TIBCO URL must change from `tcp://host:7222` to `ssl://host:7243`

#### ActiveMQ Client (ActiveMQ JMS)

```properties
# ActiveMQ SSL transport URI
activemq.broker-url=ssl://host:61617?ssl.trustStore=/path/to/truststore.jks&ssl.trustStorePassword=<password>
```

- [ ] Backend needs truststore mounted
- [ ] Connection URL must switch from `tcp://` to `ssl://`

### Backend Values Changes Needed

```yaml
# values.yaml — backend section needs these populated when SSL enabled
backend:
  certificateVolumes:
    - name: messaging-certs
      secretName: org-wide-cert       # same secret used by optional services
      mountPath: /etc/ssl/messaging
  env:
    # Kafka
    SPRING_KAFKA_SECURITY_PROTOCOL: "SSL"
    SPRING_KAFKA_SSL_TRUST_STORE_LOCATION: "file:/etc/ssl/messaging/truststore.jks"
    # TIBCO
    TIBCO_URL: "ssl://tibco-svc:7243"   # instead of tcp://
    TIBCO_SSL_CERT_PATH: "/etc/ssl/messaging/tls.crt"
    # IBM MQ
    IBMMQ_SSL_CIPHER_SUITE: "TLS_RSA_WITH_AES_256_GCM_SHA384"
    # ActiveMQ
    ACTIVEMQ_BROKER_URL: "ssl://activemq-svc:61617"
```

### Summary: Backend Work Items

- [ ] Support SSL/TLS in all four messaging protocol implementations in the Java backend
- [ ] Add conditional SSL configuration loading (plain TCP when `ssl.enabled=false`, TLS when `true`)
- [ ] Add `certificateVolumes` wiring in the Helm chart backend template to auto-mount certs when `global.certificates.enabled=true`
- [ ] Update connection URL env vars to switch between `tcp://` and `ssl://` / `SSL` protocol based on global SSL flag
- [ ] Handle truststore/keystore format requirements per protocol (JKS for Kafka/MQ/ActiveMQ, PEM for TIBCO)

---

## 9. Helm Chart TLS Gaps — Findings

Complete gap analysis from reviewing all optional service templates.

### Already Implemented

- Global SSL toggle: `global.certificates.enabled`
- Three certificate provisioning methods: `secret`, `api`, `embedded`
- Per-service `$instanceSslEnabled` conditional logic in all four templates
- Certificate init container helper (`api` method)
- `certificateVolume` / `certificateVolumeMount` helpers in `_helpers.tpl`
- SSL env vars conditionally injected for each service when enabled
- `embeddedCertSecret` helper for dev/testing

### Gaps Per Service

#### Kafka (`kafka.yaml`)

| # | Gap | Severity |
|---|---|---|
| 1 | PEM → JKS conversion not implemented; Kafka requires JKS keystores | High |
| 2 | Keystore passwords hardcoded as `"changeit"` — not overridable | High |
| 3 | `KAFKA_ADVERTISED_LISTENERS` uses internal K8s hostname — blocks external SNI routing | Critical |
| 4 | No `VirtualService` resource in template | Critical |

#### IBM MQ (`ibmmq.yaml`)

| # | Gap | Severity |
|---|---|---|
| 1 | `MQ_SSLKEYR` expects KDB format; cert helpers produce PEM | High |
| 2 | No `VirtualService` resource in template | Critical |

#### TIBCO (`tibco.yaml`)

| # | Gap | Severity |
|---|---|---|
| 1 | `service.sslPort` defined in values but Service template always uses `tcpPort` regardless of SSL | Medium |
| 2 | No `VirtualService` resource in template | Critical |

#### ActiveMQ (`activemq.yaml`)

| # | Gap | Severity |
|---|---|---|
| 1 | PEM → JKS conversion not implemented; ActiveMQ requires JKS keystores | High |
| 2 | Keystore passwords hardcoded as `"changeit"` — not overridable | High |
| 3 | Service `targetPort` hardcoded to `61616`; SSL standard port is `61617` | Medium |
| 4 | No `VirtualService` resource in template | Critical |

#### Cross-Cutting

| # | Gap | Affects |
|---|---|---|
| 1 | No VirtualService templates in any optional service | All four |
| 2 | Istio Gateway (`gateway.tf`) only has HTTP port 80; no TLS/443 entry | All TCP services |
| 3 | Backend cert volumes not auto-wired when SSL enabled | Core product |

---

## 10. Bring Your Own Domain (BYOD)

### Concept

Instead of NodePorts with IP addresses, teams use real corporate DNS names wired directly
into the Istio Gateway with a proper TLS certificate.

### How It Works

1. DNS (or GSLB) resolves `*.service-virt.company.com` to the Azure LB public/private IP
2. A wildcard TLS certificate for `*.service-virt.company.com` is stored as a K8s Secret in `istio-ingress`
3. The Istio Gateway is configured with this cert and accepts all `*.service-virt.company.com` hosts
4. Istio reads the **SNI hostname** from each TLS ClientHello and routes to the matching VirtualService
5. Teams deploy VirtualServices with their specific hostname (e.g., `kafka.margin.service-virt.company.com`)

### Domain Strategy Options

**Option A — Cluster admin owns the wildcard (simpler)**
```
Cluster admin: *.service-virt.company.com  (one wildcard cert covers all teams)
  Team Margin:     kafka.margin.service-virt.company.com
  Team Collateral: kafka.collateral.service-virt.company.com
```

**Option B — Each team brings their own domain (true BYOD)**
```
Team Margin:      *.margin-infra.company.com  (their cert, their DNS zone)
Team Collateral:  *.collateral-infra.company.com
```
Istio Gateway supports multiple certs simultaneously via SNI certificate selection.

### Certificate Rotation with cert-manager

```
cert-manager renews cert → updates K8s TLS Secret → Istio hot-reloads (no restart, no downtime)
```

### Port Impact

```
Without TLS:  N teams × M protocols = N×M Azure LB rules + NSG rules
With TLS+SNI: Always 1 Azure LB rule + 1 NSG rule (port 443), regardless of team/protocol count
```

# Local Deployment Guide — K3s on Vagrant

This guide walks through deploying the full Service Virtualization platform on a local K3s cluster running inside a Vagrant/VirtualBox VM. It covers cluster setup, Istio installation, TLS certificate provisioning, deploying all optional services and the core product for both teams, and running integration tests.

---

## Architecture Overview

```
Windows Host (your machine)
│
│  Port Forwarding (Vagrantfile)
│  localhost:6443  →  VM:6443   (K3s API)
│  localhost:8080  →  VM:80     (HTTP — UI and Backend APIs)
│  localhost:8443  →  VM:443    (TLS PASSTHROUGH — IBM MQ, Tibco, Kafka)
│  localhost:9443  →  VM:8443   (HTTPS — IBM MQ web consoles)
│
└─ VirtualBox VM  (Ubuntu 22.04, 8 GB RAM, 4 CPUs)
   └─ K3s (single-node Kubernetes)
      ├─ istio-ingress namespace
      │   ├─ istiod (control plane)
      │   ├─ istio-ingressgateway (NodePort: 80 / 443 / 8443)
      │   └─ shared-gateway (Gateway CRD — 3 servers: HTTP / TLS / HTTPS)
      ├─ margin namespace  (Helm release: "margin" — single umbrella chart)
      │   ├─ margin-mq-icg                             (IBM MQ, port 1414 + console 9443)
      │   ├─ margin-mq-rto                             (IBM MQ, port 1414 + console 9443)
      │   ├─ margin-service-virtualization-tibco-serverA  (Tibco EMS, ports 7222 / 7243)
      │   ├─ margin-service-virtualization-tibco-serverB  (Tibco EMS, ports 7222 / 7243)
      │   ├─ margin-service-virtualization-kafka-events   (Kafka KRaft, ports 9092 / 9094)
      │   ├─ margin-service-virtualization-kafka-analytics(Kafka KRaft, ports 9092 / 9094)
      │   ├─ margin-service-virtualization-activemq-broker1 (ActiveMQ, port 61616)
      │   ├─ margin-service-virtualization-activemq-broker2 (ActiveMQ, port 61616)
      │   ├─ margin-mongodb                            (MongoDB, port 27017 — optional)
      │   ├─ margin-service-virtualization-backend     (Spring Boot, port 8080)
      │   ├─ margin-service-virtualization-ui          (nginx, port 8080)
      │   └─ Istio VS/DRs (umbrella templates/ — same release, no subchart)
      └─ collateral namespace  (Helm release: "collateral" — same umbrella chart)
          └─ (same pod set under collateral-... names)
```

### Gateway port design

| Port | Protocol | Routing type | Used for |
|---|---|---|---|
| 80 | HTTP | Path-based VirtualService | UI (`/margin/`), Backend API (`/margin/api/`) |
| 443 | TLS PASSTHROUGH | SNI VirtualService | IBM MQ wire protocol, Tibco EMS SSL, Kafka SSL |
| 8443 | HTTPS termination | Path-based VirtualService | IBM MQ web console |

---

## Umbrella Chart Architecture

Each team's deployment is orchestrated through a single **umbrella Helm chart** that manages two concerns under one Helm release:

| Location | Purpose |
|---|---|
| `charts/sv` (packaged subchart) | Deploys all pods and services (IBM MQ, Tibco, Kafka, ActiveMQ, MongoDB, Backend, UI) |
| `templates/` (umbrella-owned) | Renders Istio VirtualServices and DestinationRules — no packaging step needed |

Because the sv subchart and the umbrella's own templates share the same release name, service names derived from `{{ .Release.Name }}` are automatically consistent. The Istio routes always point to the exact Kubernetes service names that the product chart creates. No hardcoded service names exist anywhere.

The `helm-charts/` directory contains **only the product chart** (`service-virtualization`). The Istio templates live inside each team's umbrella chart and are fully owned by that team — changes to one team's routing never affect another.

### Values file layout

Each service has **one combined values file** under `umbrella/values/` that configures both the sv subchart and the Istio templates at once:

```yaml
sv:           # sv subchart config (what to deploy)
  ibmmq:
    instances:
      icg: { ... }

networking:   # umbrella Istio templates config (how to route to it)
  ibmmq:
    instances:
      icg:
        enabled: true
        sniHost: "margin2e-icg2e-svrconn.chl.mq.ibm.com"
        consoleHost: "icg.ibmmq.margin.service-virtualization.local"
```

### Rolling upgrade pattern

To add a new service, pass its values file to `helm upgrade`. Helm only restarts pods whose spec has changed; everything else keeps running:

```powershell
# Current: IBM MQ only
helm upgrade margin . -n margin -f values/sv-global.yaml -f values/ibmmq-icg.yaml

# Add Tibco — IBM MQ pods are unaffected
helm upgrade margin . -n margin -f values/sv-global.yaml -f values/ibmmq-icg.yaml -f values/tibco-servera.yaml
```

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| VirtualBox | 7.x | https://www.virtualbox.org/wiki/Downloads |
| Vagrant | 2.3+ | https://developer.hashicorp.com/vagrant/downloads |
| kubectl | latest | https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/ |
| Helm | 3.x | https://helm.sh/docs/intro/install/ |
| Terraform | 1.6+ | https://developer.hashicorp.com/terraform/downloads |
| OpenSSL | any | Included with Git for Windows |
| Java 11+ | 11+ | Required for running integration tests |
| Maven | 3.8+ | Required for running integration tests |

Verify all tools are on your PATH:

```powershell
kubectl version --client
helm version
terraform version
openssl version
java -version
mvn -version
```

---

## Phase 1 — Start the K3s Cluster

```powershell
cd deployments\local\cluster-admin

# First run takes ~10 minutes (downloads Ubuntu box + installs K3s)
vagrant up

# Verify K3s is healthy
vagrant ssh -c "kubectl get nodes"
```

Expected output:
```
NAME                 STATUS   ROLES                  AGE   VERSION
k3s-cluster-vagrant  Ready    control-plane,master   2m    v1.28.x
```

The Vagrantfile generates two kubeconfig files in `cluster-admin/`:

| File | When to use |
|---|---|
| `kubeconfig-local.yaml` | From inside the VM or via `127.0.0.1:6443` port-forward |
| `kubeconfig-bridge.yaml` | From your Windows host via the VM's bridge network IP |

Set your kubeconfig on the Windows host:

```powershell
$env:KUBECONFIG = "$(Get-Location)\deployments\local\cluster-admin\kubeconfig-bridge.yaml"
kubectl get nodes
```

> If `kubeconfig-bridge.yaml` fails, find the VM's bridge IP with:
> `vagrant ssh -c "hostname -I"` and update the server URL in the file.

---

## Phase 2 — Install Istio and Create Namespaces via Terraform

Terraform is split into two folders under `cluster-admin/terraform/istio/`:

| Folder | What it does |
|---|---|
| `base/` | Creates team namespaces + RBAC, installs Istio CRDs, Istiod, and the Ingress Gateway |
| `shared-gateway/` | Creates the shared `Gateway` resource — run after `base/` once the CRDs exist |

The split is required because `kubernetes_manifest` validates the `Gateway` resource against live CRDs at plan time. The Gateway CRD only exists after Istiod is running.

> Before applying: update `kubeconfig_path` in both `terraform.tfvars` files to use `kubeconfig-bridge.yaml` when running Terraform from Windows.

### Step 2a — base (Istio + namespaces)

```powershell
cd deployments\local\cluster-admin\terraform\istio\base
terraform init
terraform apply
```

### Step 2b — shared-gateway (Gateway resource)

```powershell
cd deployments\local\cluster-admin\terraform\istio\shared-gateway
terraform init
terraform apply
```

Verify:

```powershell
kubectl get pods -n istio-system
kubectl get pods -n istio-ingress
kubectl get namespaces margin collateral
kubectl get gateway.networking.istio.io -n istio-ingress
```

Note the NodePort values — you will need these throughout:

```powershell
$TLS_NODEPORT   = kubectl get svc istio-ingressgateway -n istio-ingress -o 'jsonpath={.spec.ports[?(@.name==\"tls\")].nodePort}'
$HTTPS_NODEPORT = kubectl get svc istio-ingressgateway -n istio-ingress -o 'jsonpath={.spec.ports[?(@.name==\"https\")].nodePort}'
$HTTP_NODEPORT  = kubectl get svc istio-ingressgateway -n istio-ingress -o 'jsonpath={.spec.ports[?(@.name==\"http2\")].nodePort}'

Write-Host "TLS NodePort  (IBM MQ / Tibco / Kafka): $TLS_NODEPORT"
Write-Host "HTTPS NodePort (web consoles):          $HTTPS_NODEPORT"
Write-Host "HTTP NodePort  (UI / Backend API):      $HTTP_NODEPORT"
```

---

## Phase 3 — Generate and Load TLS Certificates

A single wildcard certificate covers all hostnames under `*.service-virtualization.local`. It is loaded into three namespaces: `istio-ingress` (for the Gateway), `margin`, and `collateral` (for service pods and the backend truststore).

```powershell
# Generate self-signed wildcard certificate (valid 2 years)
openssl req -x509 -newkey rsa:4096 -keyout wildcard.key -out wildcard.crt `
  -days 730 -nodes `
  -subj "/CN=*.service-virtualization.local" `
  -addext "subjectAltName=DNS:*.service-virtualization.local"

# Gateway TLS secret (Istio terminates HTTPS on port 8443)
kubectl create secret tls gateway-tls-cert --cert=wildcard.crt --key=wildcard.key -n istio-ingress

# Per-team TLS secrets (mounted into service pods for their own TLS listeners)
kubectl create secret tls margin-tls-secret --cert=wildcard.crt --key=wildcard.key -n margin

kubectl create secret tls collateral-tls-secret --cert=wildcard.crt --key=wildcard.key -n collateral
```

Keep `wildcard.crt` — you will need it in Phase 11 to build the Java test truststore.

---

## Phase 4 — Umbrella Chart First-Time Setup

The umbrella chart depends on the product chart (`sv`). Run `helm dependency update` once per team to download and package it into the `charts/` directory. Re-run only when you bump the product chart version in `Chart.yaml`.

```powershell
helm dependency update deployments/local/teams/teams/margin/umbrella
helm dependency update deployments/local/teams/teams/collateral/umbrella
```

> This creates `charts/service-virtualization-1.0.0.tgz` inside each umbrella `charts/` directory. The `.tgz` file is gitignored — every developer runs this command after cloning. The Istio templates and RBAC templates in `templates/` are plain YAML files owned by the umbrella chart and require no packaging step; edits take effect on the next `helm upgrade`.

---

## Phases 5–8 — Deploy All Services (Rolling Upgrade)

All resources — RBAC (`ServiceAccount` + `RoleBinding`), messaging, MongoDB, backend, UI, **and their Istio routing** — are deployed via a single `helm upgrade --install` per team. RBAC is rendered from `umbrella/templates/rbac.yaml` on the very first install and is idempotent on every subsequent upgrade. Each phase below adds one or more values files to the same release.

> All `helm upgrade` commands below are run from the **workspace root** (`c:\Users\ambuj\ServiceVirtualization`).

---

## Phase 5 — Step 1: IBM MQ

```powershell
$MARGIN_UMBRELLA  = "deployments/local/teams/teams/margin/umbrella"
$COLLATERAL_UMBRELLA = "deployments/local/teams/teams/collateral/umbrella"

# Margin
helm upgrade --install margin $MARGIN_UMBRELLA -f $MARGIN_UMBRELLA/values/sv-global.yaml -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml -f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml --namespace margin

# Collateral
helm upgrade --install collateral $COLLATERAL_UMBRELLA -f $COLLATERAL_UMBRELLA/values/sv-global.yaml -f $COLLATERAL_UMBRELLA/values/ibmmq-icg.yaml -f $COLLATERAL_UMBRELLA/values/ibmmq-rto.yaml --namespace collateral

# IBM MQ takes ~2 minutes to initialise — wait before proceeding
kubectl wait --for=condition=available deployment/margin-mq-icg -n margin --timeout=180s
kubectl wait --for=condition=available deployment/margin-mq-rto -n margin --timeout=180s
```

IBM MQ VirtualServices and DestinationRules are rendered by the umbrella's own `templates/ibmmq.yaml` — no separate `kubectl apply` needed.

---

## Phase 6 — Step 2: Add Tibco EMS

> **Required: `certificates.password`**
> When `certificates.enabled: true`, you **must** set `certificates.password` in every Tibco instance values file.
> TIBCO EMS requires a non-empty `ssl_password` value — it does not accept blank and will try to prompt stdin, which fails in a container.
> For unencrypted private keys (the common case) any non-empty string works, e.g. `password: "changeit"`.
> For encrypted private keys set this to the actual passphrase used when generating the key.

```powershell
# Margin — same release, add Tibco values files; IBM MQ pods are unaffected
helm upgrade margin $MARGIN_UMBRELLA -f $MARGIN_UMBRELLA/values/sv-global.yaml -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml -f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml -f $MARGIN_UMBRELLA/values/tibco-servera.yaml -f $MARGIN_UMBRELLA/values/tibco-serverb.yaml --namespace margin

# Collateral
helm upgrade collateral $COLLATERAL_UMBRELLA `
  -f $COLLATERAL_UMBRELLA/values/sv-global.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-icg.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-rto.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-servera.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-serverb.yaml `
  --namespace collateral

kubectl get pods -n margin -w
```

---

## Phase 7 — Step 3: Add Kafka

```powershell
helm upgrade margin $MARGIN_UMBRELLA -f $MARGIN_UMBRELLA/values/sv-global.yaml -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml-f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml -f $MARGIN_UMBRELLA/values/tibco-servera.yaml  -f $MARGIN_UMBRELLA/values/tibco-serverb.yaml -f $MARGIN_UMBRELLA/values/kafka-events.yaml -f $MARGIN_UMBRELLA/values/kafka-analytics.yaml --namespace margin

helm upgrade collateral $COLLATERAL_UMBRELLA `
  -f $COLLATERAL_UMBRELLA/values/sv-global.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-icg.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-rto.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-servera.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-serverb.yaml `
  -f $COLLATERAL_UMBRELLA/values/kafka-events.yaml `
  --namespace collateral

kubectl get pods -n margin -w
```

---

## Phase 8 — Step 4: Add ActiveMQ + MongoDB + Core Product

### Step 8a — Add ActiveMQ

```powershell
helm upgrade margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-servera.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-serverb.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-events.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-analytics.yaml `
  -f $MARGIN_UMBRELLA/values/activemq.yaml `
  --namespace margin

helm upgrade collateral $COLLATERAL_UMBRELLA `
  -f $COLLATERAL_UMBRELLA/values/sv-global.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-icg.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-rto.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-servera.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-serverb.yaml `
  -f $COLLATERAL_UMBRELLA/values/kafka-events.yaml `
  -f $COLLATERAL_UMBRELLA/values/activemq.yaml `
  --namespace collateral
```

### Step 8b — Add MongoDB

MongoDB is an optional service — teams with an existing MongoDB cluster, Azure Cosmos DB, or Sybase should skip this step and point `MONGODB_URI` in `backend.yaml` at their own instance.

```powershell
helm upgrade margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-servera.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-serverb.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-events.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-analytics.yaml `
  -f $MARGIN_UMBRELLA/values/activemq.yaml `
  -f $MARGIN_UMBRELLA/values/mongodb.yaml `
  --namespace margin

  helm upgrade margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-events.yaml `
  -f $MARGIN_UMBRELLA/values/mongodb.yaml `
  --namespace margin

helm upgrade collateral $COLLATERAL_UMBRELLA `
  -f $COLLATERAL_UMBRELLA/values/sv-global.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-icg.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-rto.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-servera.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-serverb.yaml `
  -f $COLLATERAL_UMBRELLA/values/kafka-events.yaml `
  -f $COLLATERAL_UMBRELLA/values/activemq.yaml `
  -f $COLLATERAL_UMBRELLA/values/mongodb.yaml `
  --namespace collateral

# Wait for MongoDB before deploying the backend
kubectl wait --for=condition=available deployment/margin-mongodb -n margin --timeout=120s
kubectl wait --for=condition=available deployment/collateral-mongodb -n collateral --timeout=120s
```

> `storage.enabled: false` means MongoDB uses `emptyDir` — data is lost on pod restart. Set `storage.enabled: true` with `size`/`storageClass` for persistent storage.

### Step 8c — Add Backend and UI (final upgrade)

```powershell
# Margin — complete deployment: all services + core product + all Istio routing
helm upgrade margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-servera.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-serverb.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-events.yaml `
  -f $MARGIN_UMBRELLA/values/kafka-analytics.yaml `
  -f $MARGIN_UMBRELLA/values/activemq.yaml `
  -f $MARGIN_UMBRELLA/values/mongodb.yaml `
  -f $MARGIN_UMBRELLA/values/backend.yaml `
  -f $MARGIN_UMBRELLA/values/ui.yaml `
  --namespace margin

# Collateral
helm upgrade collateral $COLLATERAL_UMBRELLA `
  -f $COLLATERAL_UMBRELLA/values/sv-global.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-icg.yaml `
  -f $COLLATERAL_UMBRELLA/values/ibmmq-rto.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-servera.yaml `
  -f $COLLATERAL_UMBRELLA/values/tibco-serverb.yaml `
  -f $COLLATERAL_UMBRELLA/values/kafka-events.yaml `
  -f $COLLATERAL_UMBRELLA/values/activemq.yaml `
  -f $COLLATERAL_UMBRELLA/values/mongodb.yaml `
  -f $COLLATERAL_UMBRELLA/values/backend.yaml `
  -f $COLLATERAL_UMBRELLA/values/ui.yaml `
  --namespace collateral

kubectl get pods -n margin -w
```

Backend and UI VirtualServices are rendered by the umbrella's own `templates/backend-ui.yaml` — no separate `kubectl apply` needed.

---

## Phase 9 — Configure DNS on Windows

Istio routes TLS traffic by SNI hostname — the client must present the correct hostname during the TLS handshake. HTTP traffic is routed by path prefix.

Find your VM's bridge IP:

```powershell
vagrant ssh -c "hostname -I | awk '{print $1}'"
# e.g. 192.168.1.105 — use this as <VM-IP> below
```

### Option A — Acrylic DNS Proxy (recommended)

Acrylic is a free local DNS proxy that supports wildcard entries. Install it once and you never need to update a hosts file when adding new services.

1. Install [Acrylic DNS Proxy](https://mayakron.altervista.org/support/acrylic/Home.htm)
2. Edit `C:\Program Files\Acrylic DNS Proxy\AcrylicHosts.txt`
3. Add one wildcard entry:
   ```
   <VM-IP>  *.service-virtualization.local
   ```
4. Restart the service:
   ```powershell
   net stop AcrylicDNSProxySvc
   net start AcrylicDNSProxySvc
   ```
5. Set `127.0.0.1` as your first DNS server in Windows Network Adapter settings

### Option B — Windows hosts file

Edit `C:\Windows\System32\drivers\etc\hosts` as Administrator (no wildcard support — one entry per hostname):

```
<VM-IP>  icg.ibmmq.margin.service-virtualization.local
<VM-IP>  rto.ibmmq.margin.service-virtualization.local
<VM-IP>  icg.ibmmq.collateral.service-virtualization.local
<VM-IP>  rto.ibmmq.collateral.service-virtualization.local
<VM-IP>  servera.tibco.margin.service-virtualization.local
<VM-IP>  serverb.tibco.margin.service-virtualization.local
<VM-IP>  servera.tibco.collateral.service-virtualization.local
<VM-IP>  serverb.tibco.collateral.service-virtualization.local
<VM-IP>  events.kafka.margin.service-virtualization.local
<VM-IP>  analytics.kafka.margin.service-virtualization.local
<VM-IP>  events.kafka.collateral.service-virtualization.local
<VM-IP>  analytics.kafka.collateral.service-virtualization.local
```

> Use the **bridge IP** (e.g., `192.168.x.x`), not `127.0.0.1`. The port-forwards in the Vagrantfile use `127.0.0.1`, but those only cover `6443`, `8080`, `8443`, and `9443` — not arbitrary NodePorts.

Verify:

```powershell
Resolve-DnsName icg.ibmmq.margin.service-virtualization.local
# Should return <VM-IP>
```

---

## Phase 10 — End-to-End Verification

### UI and Backend API

```powershell
# Get HTTP NodePort
$HTTP_NODEPORT = kubectl get svc istio-ingressgateway -n istio-ingress `
  -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}'

# UI should load (navigate in browser)
# http://<VM-IP>:$HTTP_NODEPORT/margin/

# Backend health check
curl.exe http://<VM-IP>:$HTTP_NODEPORT/margin/api/actuator/health
```

### IBM MQ — TLS handshake (wire protocol)

```powershell
$TLS_NODEPORT = kubectl get svc istio-ingressgateway -n istio-ingress `
  -o jsonpath='{.spec.ports[?(@.name=="tls")].nodePort}'

openssl s_client `
  -connect icg.ibmmq.margin.service-virtualization.local:$TLS_NODEPORT `
  -servername icg.ibmmq.margin.service-virtualization.local

# Expected: certificate CN=*.service-virtualization.local
# "Verify return code: 18 (self signed certificate)" is expected for local dev
```

### IBM MQ — web console (HTTPS)

```powershell
$HTTPS_NODEPORT = kubectl get svc istio-ingressgateway -n istio-ingress `
  -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}'

curl.exe -k -L https://icg.ibmmq.margin.service-virtualization.local:$HTTPS_NODEPORT/margin/mq-icg/ibmmq/console/
# Expected: HTTP 200 with IBM MQ login page HTML
```

Console credentials: `admin` / `admin`

### Tibco EMS — TLS handshake

```powershell
openssl s_client `
  -connect servera.tibco.margin.service-virtualization.local:$TLS_NODEPORT `
  -servername servera.tibco.margin.service-virtualization.local
```

### Kafka — SSL handshake

```powershell
openssl s_client `
  -connect events.kafka.margin.service-virtualization.local:$TLS_NODEPORT `
  -servername events.kafka.margin.service-virtualization.local
```

---

## Phase 11 — Run Integration Tests

The IBM MQ integration test is at `test/ibmmq-tls-test/`.

Build the test truststore from the wildcard certificate generated in Phase 3:

```powershell
keytool -import -file wildcard.crt `
  -alias gateway-ca `
  -keystore test\ibmmq-tls-test\src\test\resources\certs\truststore.jks `
  -storepass changeit -noprompt
```

Update `test/ibmmq-tls-test/src/test/resources/test.properties`:

```properties
margin.mq.host=icg.ibmmq.margin.service-virtualization.local
margin.mq.port=<TLS_NODEPORT>
margin.mq.channel=DEV.APP.SVRCONN
margin.mq.queueManager=QM_ICG
margin.mq.queue=DEV.QUEUE.1
```

Run:

```powershell
cd test\ibmmq-tls-test
mvn test `
  -Dcom.ibm.mq.cfg.preferTLS=true `
  -Djavax.net.ssl.trustStore=src\test\resources\certs\truststore.jks `
  -Djavax.net.ssl.trustStorePassword=changeit
```

---

## Quick Reference

### Helm dry-run (preview rendered templates)

```powershell
$MARGIN_UMBRELLA = "deployments/local/teams/teams/margin/umbrella"

# Preview IBM MQ + networking templates
helm template margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-rto.yaml `
  --namespace margin

# Preview only Istio networking templates
helm template margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/ibmmq-icg.yaml `
  --namespace margin --show-only templates/ibmmq.yaml
```

### Refresh the sv subchart after any product chart change

The umbrella chart packages `service-virtualization` as a `.tgz` inside `umbrella/charts/`.
Any change to files under `helm-charts/service-virtualization/` (templates, `values.yaml`, `_helpers.tpl`)
is **not picked up by `helm upgrade` until you re-package the dependency**:

```powershell
# Must re-run whenever helm-charts/service-virtualization/** is changed
helm dependency update deployments/local/teams/teams/margin/umbrella
helm dependency update deployments/local/teams/teams/collateral/umbrella
```

Common triggers: template bug fixes, adding a new optional service, changing a default value.
Symptoms of a stale package: `helm upgrade` succeeds but uses old behaviour / old template output.

### Check cluster state

```powershell
# All pods across all namespaces
kubectl get pods -A

# All VirtualServices and DestinationRules
kubectl get virtualservice,destinationrule -A

# Istio proxy config (routing table)
istioctl proxy-config routes deploy/istio-ingressgateway -n istio-ingress

# IBM MQ logs for a specific instance
kubectl logs -n margin -l app.kubernetes.io/instance-name=icg -f

# Backend logs
kubectl logs -n margin -l app.kubernetes.io/component=backend -f
```

### Debug a Helm upgrade ("nothing happened" / "pods not created")

Use these commands when an upgrade reports success but expected resources (pods, services, VirtualServices) do not appear.

```powershell
# 1. Confirm the upgrade was applied and check its status
helm history margin -n margin
helm history collateral -n collateral

# 2. Preview exactly what Helm would render — check the output for the
#    expected Deployment / Service / VirtualService blocks.
#    If a Deployment is missing, sv.<protocol>.enabled is probably false.
helm template margin $MARGIN_UMBRELLA `
  -f $MARGIN_UMBRELLA/values/sv-global.yaml `
  -f $MARGIN_UMBRELLA/values/tibco-servera.yaml `
  --namespace margin | Select-String "kind:"

# 3. Check for Kubernetes events — rejected or failed resources appear here
kubectl get events -n margin --sort-by='.lastTimestamp' | Select-Object -Last 20

# 4. Check if a resource was left in a bad state from a prior run
kubectl get all -n margin
kubectl get virtualservice,destinationrule -n margin
```

**Common causes:**

| Symptom | Likely cause |
|---|---|
| No pods created | `sv.<protocol>.enabled: false` in the values file |
| VirtualServices missing | `networking.<protocol>.instances.<name>.enabled: false` |
| Upgrade says "no changes" | Values files unchanged — Helm does a no-op diff |
| Resource rejected silently | Invalid name (e.g. uppercase) — check `kubectl get events` |

### Full teardown

```powershell
# Uninstall the single umbrella release per team (removes pods AND Istio resources)
helm uninstall margin -n margin
helm uninstall collateral -n collateral
helm uninstall margin-rbac -n margin
helm uninstall collateral-rbac -n collateral

# Destroy Terraform (Gateway first, then base)
cd deployments\local\cluster-admin\terraform\istio\shared-gateway; terraform destroy
cd deployments\local\cluster-admin\terraform\istio\base; terraform destroy

# Stop or destroy the VM
vagrant halt      # stop, keep data
vagrant destroy   # delete everything
```

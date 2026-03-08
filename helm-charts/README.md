# Service Virtualization Platform - Helm Charts

## Overview

This directory contains the **official Helm charts** for the Service Virtualization Platform.

These charts are published to a Helm registry and consumed by customers via `helm install`.

## Charts

### 1. service-virtualization (Core Product)

**Path:** `service-virtualization/`

**Description:** The main application chart containing:
- Backend API
- UI (React application)
- IBM MQ instances
- MongoDB database
- Kafka event streaming
- ActiveMQ messaging
- TIBCO EMS messaging

**Installation:**
```bash
helm install myapp yourcompany/service-virtualization \
  -f values.yaml \
  -n my-namespace
```

**Documentation:** See [`service-virtualization/README.md`](service-virtualization/README.md)

---

### 2. service-virtualization-istio (Optional Companion)

**Path:** `service-virtualization-istio/`

**Description:** Istio networking companion chart for:
- VirtualServices for IBM MQ web consoles
- Optional backend API routing
- Optional UI routing

**Installation:**
```bash
# Install AFTER core chart
helm install myapp-networking yourcompany/service-virtualization-istio \
  -f values.yaml \
  -n my-namespace
```

**Documentation:** See [`service-virtualization-istio/README.md`](service-virtualization-istio/README.md)

---

## Chart Publishing

### Local Development

Use ChartMuseum for local testing:

```bash
# 1. Start local registry
cd ../chartmuseum/scripts
./start.ps1  # Windows
# or
./start.sh   # Linux/Mac

# 2. Publish charts
./publish-charts.ps1  # Windows
# or
./publish-charts.sh   # Linux/Mac

# 3. Add repo
helm repo add yourcompany http://localhost:8080
helm repo update

# 4. Search charts
helm search repo yourcompany
```

### Production Publishing

For production, publish to:

- **GitHub Packages (OCI):**
  ```bash
  helm package service-virtualization
  helm push service-virtualization-1.0.0.tgz oci://ghcr.io/yourcompany
  ```

- **Azure Container Registry:**
  ```bash
  az acr helm push service-virtualization-1.0.0.tgz --name yourregistry
  ```

- **AWS ECR:**
  ```bash
  aws ecr get-login-password | helm registry login --username AWS --password-stdin <account>.dkr.ecr.<region>.amazonaws.com
  helm push service-virtualization-1.0.0.tgz oci://<account>.dkr.ecr.<region>.amazonaws.com/helm
  ```

---

## Customer Workflow

### Step 1: Add Registry

```bash
helm repo add yourcompany https://charts.yourcompany.com
helm repo update
```

### Step 2: Search Charts

```bash
helm search repo yourcompany
# NAME                                   CHART VERSION  APP VERSION
# yourcompany/service-virtualization     1.0.0          1.0.0
# yourcompany/service-virtualization-istio 1.0.0        1.0.0
```

### Step 3: Install Core Chart

```bash
helm install myapp yourcompany/service-virtualization \
  -f values.yaml \
  -n my-namespace
```

### Step 4: Install Networking (Optional)

```bash
# Only if using Istio
helm install myapp-networking yourcompany/service-virtualization-istio \
  -f values.yaml \
  -n my-namespace
```

---

## Chart Development

### Directory Structure

```
helm-charts/
├── service-virtualization/          # Core product chart
│   ├── Chart.yaml                   # Chart metadata
│   ├── values.yaml                  # Default values
│   ├── README.md                    # Chart documentation
│   └── templates/                   # Kubernetes manifests
│       ├── backend.yaml
│       ├── ui.yaml
│       ├── optional-services/
│       │   ├── ibmmq.yaml
│       │   ├── mongodb.yaml
│       │   ├── kafka.yaml
│       │   ├── activemq.yaml
│       │   └── tibco.yaml
│       ├── serviceaccount.yaml
│       └── _helpers.tpl
│
├── service-virtualization-istio/    # Istio companion chart
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── README.md
│   └── templates/
│       └── virtualservices.yaml
│
└── README.md                        # This file
```

### Testing Charts Locally

**Lint charts:**
```bash
helm lint service-virtualization
helm lint service-virtualization-istio
```

**Template validation:**
```bash
helm template test service-virtualization -f values.yaml | kubectl apply --dry-run=client -f -
```

**Install locally:**
```bash
# Core chart
helm install test-app ./service-virtualization -f values.yaml -n test

# Companion chart
helm install test-networking ./service-virtualization-istio \
  -f values.yaml \
  --set appReleaseName=test-app \
  --set teamName=test \
  -n test
```

**Upgrade:**
```bash
# Make changes to templates
vim service-virtualization/templates/backend.yaml

# Upgrade
helm upgrade test-app ./service-virtualization -f values.yaml
```

**Uninstall:**
```bash
helm uninstall test-networking
helm uninstall test-app
```

### Versioning

Follow semantic versioning in `Chart.yaml`:

```yaml
version: 1.0.0  # Chart version
appVersion: "1.0.0"  # Application version
```

**Version bumping:**
- **Patch** (1.0.0 → 1.0.1): Bug fixes, documentation
- **Minor** (1.0.0 → 1.1.0): New features, backward compatible
- **Major** (1.0.0 → 2.0.0): Breaking changes

### Best Practices

1. **Keep charts infrastructure-agnostic**
   - Core chart has NO Istio resources
   - Networking in separate companion chart

2. **Use clear naming conventions**
   - Core chart: `service-virtualization`
   - Companions: `service-virtualization-{type}`

3. **Document everything**
   - README for each chart
   - Comments in values.yaml
   - Examples in documentation

4. **Test thoroughly**
   - Lint before publishing
   - Test install/upgrade/rollback
   - Test with different values

5. **Version properly**
   - Bump version for every change
   - Follow semantic versioning
   - Tag releases in Git

---

## Chart Dependencies

### Core Chart

No external dependencies. All services are bundled.

### Istio Companion

**Requires:**
- Core chart must be installed first
- Istio must be installed in cluster
- Shared Gateway must exist

**Dependencies in `values.yaml`:**
```yaml
appReleaseName: ""  # Must match core chart release name
```

---

## Chart Configuration

### Core Chart

See [`service-virtualization/values.yaml`](service-virtualization/values.yaml) for all configuration options.

**Key configurations:**
- Backend/UI replica counts
- IBM MQ instances
- MongoDB, Kafka, ActiveMQ, TIBCO
- Resource limits
- Health checks

### Istio Companion

See [`service-virtualization-istio/values.yaml`](service-virtualization-istio/values.yaml) for all configuration options.

**Key configurations:**
- appReleaseName (must match core chart)
- teamName (for hostname generation)
- domain
- Gateway reference
- IBM MQ instances to route

---

## Support

- **Core Chart Issues:** Check service-virtualization/README.md
- **Networking Issues:** Check service-virtualization-istio/README.md
- **Publishing Issues:** Check ../chartmuseum/README.md
- **Helm Issues:** https://helm.sh/docs/

---

## Quick Reference

```bash
# Local Development
cd ../chartmuseum/scripts && ./start.ps1
./publish-charts.ps1
helm repo add yourcompany http://localhost:8080

# Install
helm install myapp yourcompany/service-virtualization -f values.yaml
helm install myapp-networking yourcompany/service-virtualization-istio -f values.yaml

# Upgrade
helm upgrade myapp yourcompany/service-virtualization -f values.yaml

# Rollback
helm rollback myapp

# Uninstall
helm uninstall myapp-networking
helm uninstall myapp
```


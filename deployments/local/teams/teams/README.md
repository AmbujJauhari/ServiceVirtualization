# Teams - Service Virtualization Deployments

## 🎯 Architecture: Decoupled Product & Infrastructure

```
┌─────────────────────────────────────────────────────────────┐
│ Product: helm-charts/service-virtualization                 │
│ - Infrastructure-agnostic                                   │
│ - IBM MQ, Kafka, PostgreSQL, TIBCO, ActiveMQ               │
│ - NO networking dependencies                                │
└─────────────────────────────────────────────────────────────┘
                            ↓
                 Teams Deploy (2-3 Steps)
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ Step 1: Deploy Application                                   │
│   helm install <team>-app helm-charts/service-virtualization │
│     -f teams/<team>/app-values.yaml                          │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ Step 2: Deploy Networking (OPTIONAL - Team's Choice)         │
│   Option A: Istio                                            │
│     kubectl apply -f teams/<team>/networking/istio/          │
│   Option B: Kubernetes Ingress                               │
│     kubectl apply -f teams/<team>/networking/ingress/        │
│   Option C: None                                             │
│     Skip - Internal only                                     │
└──────────────────────────────────────────────────────────────┘
```

---

## 📁 Directory Structure

```
teams/
├── README.md                          ← This file
│
├── margin/
│   ├── README.md                      ← Margin team docs
│   ├── app-values.yaml                ← Product configuration
│   └── networking/
│       └── istio/
│           └── virtualservices.yaml   ← Istio routing (optional)
│
└── collateral/
    ├── README.md                      ← Collateral team docs
    ├── app-values.yaml                ← Product configuration
    └── networking/
        └── istio/
            └── virtualservices.yaml   ← Istio routing (optional)
```

---

## 🚀 Quick Start

### Deploy Margin Team

```bash
# Step 1: Deploy application
helm install margin-app ./helm-charts/service-virtualization \
  -f teams/margin/app-values.yaml \
  -n margin --create-namespace

# Step 2: Deploy networking (if external access needed)
kubectl apply -f teams/margin/networking/istio/
```

### Deploy Collateral Team

```bash
# Step 1: Deploy application
helm install collateral-app ./helm-charts/service-virtualization \
  -f teams/collateral/app-values.yaml \
  -n collateral --create-namespace

# Step 2: Deploy networking (if external access needed)
kubectl apply -f teams/collateral/networking/istio/
```

---

## 🎨 Networking Options

### Option 1: Istio (Current)

```yaml
# teams/<team>/networking/istio/virtualservices.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  gateways:
  - istio-ingress/shared-gateway
  hosts:
  - "*"
  http:
  - match:
    - uri:
        prefix: "/<team>/mq-icg/"
    route:
    - destination:
        host: <team>-app-mq-icg.<team>.svc.cluster.local
```

**Deploy:**
```bash
kubectl apply -f teams/<team>/networking/istio/
```

---

### Option 2: Kubernetes Ingress

Create `teams/<team>/networking/ingress/ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: <team>-ingress
  namespace: <team>
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - http:
      paths:
      - path: /<team>/mq-icg
        pathType: Prefix
        backend:
          service:
            name: <team>-app-mq-icg
            port:
              number: 9443
```

**Deploy:**
```bash
kubectl apply -f teams/<team>/networking/ingress/
```

---

### Option 3: No External Access

Skip networking step entirely. Services are only accessible within the cluster:

```bash
# Inside cluster:
curl http://margin-app-mq-icg.margin.svc.cluster.local:9443
```

---

## 📦 CI/CD Pipeline Integration

### Option A: Multi-Stage Pipeline

```yaml
# .gitlab-ci.yml
stages:
  - deploy-app
  - deploy-networking

deploy-margin-app:
  stage: deploy-app
  script:
    - helm upgrade --install margin-app ./helm-charts/service-virtualization
      -f teams/margin/app-values.yaml -n margin

deploy-margin-networking:
  stage: deploy-networking
  script:
    - kubectl apply -f teams/margin/networking/istio/
```

### Option B: Single Command (Helmfile)

```yaml
# helmfile.yaml
releases:
  - name: margin-app
    chart: ./helm-charts/service-virtualization
    values:
      - teams/margin/app-values.yaml
    namespace: margin

  - name: margin-networking
    chart: ./teams/margin/networking-chart  # Optional Helm chart wrapper
    namespace: margin
    needs:
      - margin-app
```

---

## 🔒 RBAC Integration

Teams have access only to their namespace:

```bash
# Margin team can only access margin namespace
kubectl config use-context margin
kubectl get pods -n margin       # ✅ Works
kubectl get pods -n collateral   # ❌ Forbidden
```

---

## 📝 Adding a New Team

1. **Create team folder:**
   ```bash
   mkdir -p teams/payments/networking/istio
   ```

2. **Copy template:**
   ```bash
   cp teams/margin/app-values.yaml teams/payments/
   ```

3. **Customize configuration:**
   ```bash
   # Edit teams/payments/app-values.yaml
   # Change team name, namespace, services, etc.
   ```

4. **Create networking config:**
   ```bash
   # Copy and edit
   cp teams/margin/networking/istio/virtualservices.yaml \
      teams/payments/networking/istio/
   ```

5. **Deploy:**
   ```bash
   helm install payments-app ./helm-charts/service-virtualization \
     -f teams/payments/app-values.yaml -n payments --create-namespace
   
   kubectl apply -f teams/payments/networking/istio/
   ```

---

## ✅ Benefits of This Architecture

### **1. Product Independence**
- ✅ Helm chart works in any Kubernetes cluster
- ✅ No Istio/Ingress dependency
- ✅ Portable across environments

### **2. Team Flexibility**
- ✅ Choose Istio, Ingress, or no networking
- ✅ Customize networking per team
- ✅ Internal-only deployments possible

### **3. Clear Separation**
- ✅ Product = Application services
- ✅ Infrastructure = Team's choice
- ✅ Independent versioning

### **4. Simplified CI/CD**
- ✅ Product chart deployed once per team
- ✅ Networking updated independently
- ✅ No complex dependencies

---

## 🔍 Troubleshooting

### Application deployed but not accessible

```bash
# Check if application is running
kubectl get pods -n <team>

# Check if networking is deployed
kubectl get virtualservices -n <team>  # For Istio
kubectl get ingress -n <team>          # For Ingress
```

### Istio VirtualService not working

```bash
# Check Gateway exists
kubectl get gateway shared-gateway -n istio-ingress

# Check VirtualService references correct Gateway
kubectl get virtualservice -n <team> -o yaml | grep gateway

# Check service exists
kubectl get svc -n <team>
```

---

## 📚 Related Documentation

- Product Chart: `helm-charts/service-virtualization/README.md`
- Istio Setup: `k3s/01-cluster-admin/istio/README.md`
- Gateway Setup: `k3s/01-cluster-admin/shared-gateway/README.md`

---

## 🎯 Summary

**Product (service-virtualization):**
- Infrastructure-agnostic Helm chart
- IBM MQ, Kafka, PostgreSQL, TIBCO, ActiveMQ
- No networking dependencies

**Teams:**
- Deploy product with their configuration
- Choose their own networking (Istio, Ingress, or none)
- Full control over routing and access

This decoupled architecture ensures the product remains portable while giving teams maximum flexibility! 🚀

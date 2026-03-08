# SSL/TLS Certificate Configuration Guide

## Overview

This Service Virtualization platform supports optional SSL/TLS for all TCP-based services (TIBCO, IBM MQ, Kafka, ActiveMQ).

### Two Deployment Modes:

1. **WITH SSL (SNI-based routing)**:
   - All services on standard ports (7222, 1414, 9092, etc.)
   - Routes by SNI hostname (e.g., `ems-server-a.margin.example.com`)
   - Single LoadBalancer with custom domains (BYOD)
   - Cost: $25/month
   - End-to-end encryption

2. **WITHOUT SSL (Port-based routing)**:
   - Each service on different port (7222, 7322, 7422, etc.)
   - Routes by port number
   - Single LoadBalancer with multiple ports
   - Cost: $25/month
   - Plain TCP (no encryption)
   - Similar to HTTP path-based routing pattern

---

## Certificate Provisioning Methods

### Method 1: Pre-Created Kubernetes Secret (Recommended)

**Best for**: Production, most common use case

**Setup**:
```bash
# Extract from your org-wide JKS file
keytool -importkeystore \
  -srckeystore org-wide.jks \
  -srcstoretype JKS \
  -srcstorepass <password> \
  -destkeystore org-wide.p12 \
  -deststoretype PKCS12 \
  -deststorepass <password>

# Convert to PEM format
openssl pkcs12 -in org-wide.p12 -passin pass:<password> \
  -nokeys -out org-cert.pem

openssl pkcs12 -in org-wide.p12 -passin pass:<password> \
  -nodes -nocerts -out org-key.pem

# Create Kubernetes secret
kubectl create secret tls org-wide-cert \
  --cert=org-cert.pem \
  --key=org-key.pem \
  -n margin
```

**Configuration**:
```yaml
# teams/margin/optional-services/tibco-values.yaml
global:
  certificates:
    enabled: true              # Enable SSL
    method: secret
    secret:
      name: "org-wide-cert"    # Secret you created
      certKey: "tls.crt"
      keyKey: "tls.key"
```

---

### Method 2: API Download (Dynamic)

**Best for**: Dynamic certificate provisioning, rotation

**Setup**:
```bash
# Create API authentication secret
kubectl create secret generic cert-api-token \
  --from-literal=token="your-api-token" \
  -n margin
```

**Configuration**:
```yaml
global:
  certificates:
    enabled: true
    method: api
    api:
      url: "https://cert-api.company.com"
      endpoint: "/v1/certificates/download/{service}"
      method: "GET"
      authentication:
        enabled: true
        type: "token"
        secretName: "cert-api-token"
        secretKey: "token"
      timeout: "30s"
      certPath: "data.certificate"
      keyPath: "data.private_key"
```

**Expected API Response**:
```json
{
  "data": {
    "certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
    "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
    "ca_chain": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"
  }
}
```

---

### Method 3: Embedded Certificate (Dev/Testing Only)

**Best for**: Development, testing (NOT for production)

**Configuration**:
```yaml
global:
  certificates:
    enabled: true
    method: embedded
    embedded:
      certificate: |
        -----BEGIN CERTIFICATE-----
        MIIDXTCCAkWgAwIBAgIJAKJ...
        -----END CERTIFICATE-----
      privateKey: |
        -----BEGIN PRIVATE KEY-----
        MIIEvQIBADANBgkqhkiG9w0...
        -----END PRIVATE KEY-----
```

---

## Usage Examples

### Example 1: Enable SSL with Org Certificate

```yaml
# teams/margin/optional-services/tibco-values.yaml

global:
  certificates:
    enabled: true                    # ← Enable SSL
    method: secret
    secret:
      name: "org-wide-cert"          # ← Your org certificate

tibco:
  enabled: true
  instances:
  - name: server-a
    enabled: true
    # Uses global certificate config
```

**Deployment**:
```bash
# 1. Create certificate secret first
kubectl create secret tls org-wide-cert \
  --cert=org-cert.pem \
  --key=org-key.pem \
  -n margin

# 2. Deploy TIBCO with SSL enabled
helm install margin-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# 3. Connect from main app
ssl://ems-server-a.margin.example.com:7222
```

---

### Example 2: Disable SSL (Use Plain TCP with Different Ports)

```yaml
# teams/margin/optional-services/tibco-values.yaml

global:
  certificates:
    enabled: false                   # ← Disable SSL

tibco:
  enabled: true
  instances:
  - name: server-a
    enabled: true
    service:
      tcpPort: 7222                 # Port 7222
  
  - name: server-b
    enabled: true
    service:
      tcpPort: 7322                 # Port 7322 (different!)
```

**Deployment**:
```bash
# No certificate needed
helm install margin-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# Connect from main app (plain TCP)
tcp://gateway-ip:7222              # Server A
tcp://gateway-ip:7322              # Server B
```

---

### Example 3: Per-Service Certificate Override

```yaml
# teams/margin/optional-services/tibco-values.yaml

global:
  certificates:
    enabled: true
    method: secret
    secret:
      name: "default-cert"           # Default certificate

tibco:
  enabled: true
  instances:
  - name: server-a
    enabled: true
    # Uses global cert: default-cert
  
  - name: server-b
    enabled: true
    # Override with specific cert for server-b
    certificates:
      method: secret
      secret:
        name: "serverb-specific-cert"
```

---

### Example 4: API Download for Specific Service

```yaml
global:
  certificates:
    enabled: true
    method: secret               # Default: use secret
    secret:
      name: "org-wide-cert"

tibco:
  instances:
  - name: server-a
    enabled: true
    # Uses global secret
  
  - name: server-b
    enabled: true
    # Override: download from API for this instance
    certificates:
      method: api
      api:
        url: "https://cert-api.company.com"
        endpoint: "/certificates/tibco-server-b"
        authentication:
          enabled: true
          type: "token"
          secretName: "api-token"
          secretKey: "token"
```

---

## Architecture Comparison

### WITH SSL (SNI-Based Routing)

```
Client (Main App)
  ↓
  ssl://ems-server-a.margin.example.com:7222
  ↓
DNS: ems-server-a.margin.example.com → 203.0.113.10
  ↓
Azure Load Balancer (203.0.113.10:7222)
  ↓
Istio Gateway (reads SNI: "ems-server-a.margin.example.com")
  ↓
Routes to: TIBCO Server A Pod
  ↓
TLS handshake completed with pod
  ↓
Encrypted TIBCO EMS communication

Benefits:
✅ Standard ports (7222)
✅ Custom domains (BYOD)
✅ End-to-end encryption
✅ SNI-based routing
✅ Single LoadBalancer ($25/month)
```

### WITHOUT SSL (Port-Based Routing)

```
Client (Main App)
  ↓
  tcp://gateway-ip:7222  (Server A)
  tcp://gateway-ip:7322  (Server B)
  ↓
Azure Load Balancer (203.0.113.10)
  ↓
  Port 7222 → Istio Gateway → TIBCO Server A
  Port 7322 → Istio Gateway → TIBCO Server B
  ↓
Plain TCP communication (no encryption)

Benefits:
✅ No certificate management
✅ Simple setup
✅ Single LoadBalancer ($25/month)
✅ Similar to HTTP path-based routing

Trade-offs:
⚠️ Non-standard ports (7322, 7422, etc.)
⚠️ No encryption
⚠️ IP-based connections
```

---

## Istio Gateway Configuration

### WITH SSL: Single Port, SNI Routing

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: tibco-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 7222                      # Single port
      protocol: TLS
    tls:
      mode: PASSTHROUGH                 # Don't decrypt
    hosts:
    - "ems-server-a.margin.example.com" # Route by SNI
    - "ems-server-b.margin.example.com"
```

### WITHOUT SSL: Multiple Ports

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: tibco-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 7222                      # Port 1 → Server A
      protocol: TCP
    hosts:
    - "*"
  
  - port:
      number: 7322                      # Port 2 → Server B
      protocol: TCP
    hosts:
    - "*"
```

---

## Security Best Practices

### Certificate Management:
- ✅ Use organization-wide certificates (trusted CA)
- ✅ Store certificates as Kubernetes Secrets
- ✅ Use RBAC to restrict secret access
- ✅ Rotate certificates before expiry
- ❌ Never embed certificates in Git
- ❌ Never use self-signed certs in production

### Secret Storage:
```bash
# Create secret in each namespace
kubectl create secret tls org-wide-cert \
  --cert=org-cert.pem \
  --key=org-key.pem \
  -n margin

kubectl create secret tls org-wide-cert \
  --cert=org-cert.pem \
  --key=org-key.pem \
  -n collateral

# Verify secrets
kubectl get secrets -n margin | grep cert
kubectl describe secret org-wide-cert -n margin
```

---

## Troubleshooting

### Issue: SSL Handshake Fails

**Check**:
```bash
# Test SSL connection
openssl s_client -connect ems-server-a.margin.example.com:7222 \
  -servername ems-server-a.margin.example.com

# Look for:
# - "Verify return code: 0 (ok)" ✅
# - Certificate details
# - TLS version (TLS 1.2 or 1.3)
```

**Common Causes**:
- Certificate doesn't match hostname
- Certificate expired
- CA not trusted by client
- SNI not sent by client

---

### Issue: Certificate Not Found

**Check**:
```bash
# Verify secret exists
kubectl get secret org-wide-cert -n margin

# Check secret contents
kubectl get secret org-wide-cert -n margin -o yaml

# Verify pod has access
kubectl describe pod <pod-name> -n margin | grep -A 5 "Volumes:"
```

---

### Issue: API Download Fails

**Check**:
```bash
# Check init container logs
kubectl logs <pod-name> -n margin -c download-certificate

# Test API manually
curl -H "Authorization: Bearer $TOKEN" \
  https://cert-api.company.com/v1/certificates/download/tibco-server-a
```

---

## Migration Guide

### Migrating from LoadBalancer-per-Service to SNI Routing:

**Before** (Multiple LoadBalancers):
```yaml
tibco:
  instances:
  - name: server-a
    service:
      type: LoadBalancer          # ← Dedicated LB ($25/month)
      tcpPort: 7222
  
  - name: server-b
    service:
      type: LoadBalancer          # ← Dedicated LB ($25/month)
      tcpPort: 7222

Cost: $50/month (2 LBs)
```

**After** (SNI Routing):
```yaml
global:
  certificates:
    enabled: true
    method: secret
    secret:
      name: "org-wide-cert"

tibco:
  instances:
  - name: server-a
    service:
      type: ClusterIP             # ← No external LB needed
      tcpPort: 7222
  
  - name: server-b
    service:
      type: ClusterIP
      tcpPort: 7222

Cost: $25/month (shared Istio Gateway LB)
Savings: $25/month = $300/year per team!
```

---

## Quick Start Examples

### K3s (Local Development) - Without SSL:
```bash
# 1. Deploy with SSL disabled
helm install margin-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# 2. Access via NodePort
tcp://localhost:30722   # Server A
tcp://localhost:30732   # Server B
```

### AKS (Production) - With SSL:
```bash
# 1. Create certificate secret
kubectl create secret tls org-wide-cert \
  --cert=org-cert.pem \
  --key=org-key.pem \
  -n margin

# 2. Enable SSL in values
# Set: global.certificates.enabled: true

# 3. Deploy
helm install margin-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# 4. Configure DNS
# Point ems-server-a.margin.example.com → <LoadBalancer-IP>

# 5. Access via custom domain
ssl://ems-server-a.margin.example.com:7222
```

---

## Summary

| Feature | WITH SSL | WITHOUT SSL |
|---------|----------|-------------|
| **Encryption** | ✅ TLS 1.2/1.3 | ❌ Plain TCP |
| **Ports** | Standard (7222, 1414, 9092) | Different per service |
| **Routing** | SNI hostname | Port number |
| **Custom Domains** | ✅ BYOD supported | ⚠️ IP-based |
| **LoadBalancer** | 1 shared ($25/month) | 1 shared ($25/month) |
| **Setup Complexity** | Medium (need certs) | Low (no certs) |
| **Production Ready** | ✅ Enterprise | ✅ Internal only |
| **K3s Local Dev** | Optional | ✅ Default |
| **AKS Production** | ✅ Recommended | ⚠️ Not encrypted |

**Recommendation**: 
- **Development/K3s**: SSL disabled (simpler)
- **Production/AKS**: SSL enabled (secure, professional)

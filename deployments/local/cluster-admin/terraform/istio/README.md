# Istio Installation Module

## Overview

This Terraform module installs **Istio service mesh** and creates team namespaces:
- ✅ Team namespaces
- ✅ Istio base (CRDs)
- ✅ Istiod (Control Plane)
- ✅ Istio Ingress Gateway

**Note:** Gateway resources are created separately in the `../shared-gateway` module.

## Prerequisites

- K3s cluster running
- kubectl configured to access the cluster
- Terraform installed (>= 1.0)

## Quick Start

```bash
cd k3s/01-cluster-admin/istio

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Install Istio
terraform apply
```

**Next step:** Deploy the Gateway (see `../shared-gateway`)

---

## What Gets Installed

**In this order:**

0. **Team Namespaces** (e.g., `margin`, `collateral`)
   - Kubernetes namespaces for teams to deploy their applications
   - Labeled with environment and managed-by tags

0.1. **RBAC (ServiceAccounts, Roles, RoleBindings)**
   - ServiceAccount per team (e.g., `margin-sa`, `collateral-sa`)
   - Role with full namespace permissions
   - RoleBinding to grant team ServiceAccount access to their namespace only
   - Provides namespace isolation - teams cannot access other namespaces

1. **istio-base** (Helm chart in `istio-system` namespace)
   - Istio CRDs (CustomResourceDefinitions)
   - Includes: Gateway, VirtualService, DestinationRule, etc.

2. **istiod** (Helm chart in `istio-system` namespace)
   - Istio control plane (pilot, citadel, galley)
   - Handles configuration and certificate management
   - Enables service mesh features

3. **istio-ingressgateway** (Helm chart in `istio-ingress` namespace)
   - Ingress gateway pods for external traffic
   - Exposes services via NodePort (or LoadBalancer in cloud)

**Gateway resources are created separately** - see `../shared-gateway` module

## Configuration

Edit `terraform.tfvars`:

```hcl
# Namespace Configuration
team_namespaces = ["team-payments", "team-orders"]  # Add more as needed
environment     = "dev"

# Istio Version
istio_version = "1.20.0"

# Install Ingress Gateway
install_ingress_gateway = true

# Gateway Configuration
create_shared_gateway = true        # Create shared Gateway resource
gateway_name          = "shared-gateway"
gateway_hosts         = ["*.local", "*.company.com"]  # Accepted hosts
```

## Verify Installation

```bash
# Check team namespaces
kubectl get namespaces -l app.kubernetes.io/part-of=service-virtualization

# Check RBAC resources
kubectl get serviceaccounts -n margin
kubectl get serviceaccounts -n collateral
kubectl get roles -n margin
kubectl get rolebindings -n margin

# Check Helm releases
helm list -n istio-system
helm list -n istio-ingress

# Check Istio pods
kubectl get pods -n istio-system
kubectl get pods -n istio-ingress

# Check services
kubectl get svc -n istio-ingress

# Verify Istiod is ready
kubectl get deploy istiod -n istio-system

# Check Gateway resource (moved to shared-gateway module)
kubectl get gateway.networking.istio.io -n istio-ingress
kubectl describe gateway.networking.istio.io shared-gateway -n istio-ingress
```

## Team ServiceAccount Usage

### Option 1: Extract ServiceAccount Token (For CI/CD)

```powershell
# Get the ServiceAccount token for margin team
$secretName = kubectl get serviceaccount margin-sa -n margin -o jsonpath='{.secrets[0].name}'
$token = kubectl get secret $secretName -n margin -o jsonpath='{.data.token}' | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }

# Use token in CI/CD pipelines
# Set as KUBE_TOKEN environment variable in your pipeline
```

### Option 2: Create Kubeconfig for Teams

```powershell
# Script to generate team-specific kubeconfig
# This gives teams their own isolated kubeconfig file

$team = "margin"
$sa = "${team}-sa"
$namespace = $team

# Get cluster info
$clusterName = kubectl config view --minify -o jsonpath='{.clusters[0].name}'
$clusterServer = kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'
$clusterCA = kubectl config view --raw --minify -o jsonpath='{.clusters[0].cluster.certificate-authority-data}'

# Get ServiceAccount token
$secretName = kubectl get serviceaccount $sa -n $namespace -o jsonpath='{.secrets[0].name}'
$token = kubectl get secret $secretName -n $namespace -o jsonpath='{.data.token}'

# Create kubeconfig
@"
apiVersion: v1
kind: Config
clusters:
- cluster:
    certificate-authority-data: $clusterCA
    server: $clusterServer
  name: $clusterName
contexts:
- context:
    cluster: $clusterName
    namespace: $namespace
    user: $sa
  name: ${team}-context
current-context: ${team}-context
users:
- name: $sa
  user:
    token: $([System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($token)))
"@ | Out-File -FilePath "${team}-kubeconfig.yaml" -Encoding UTF8

Write-Host "✅ Kubeconfig created: ${team}-kubeconfig.yaml"
Write-Host "Give this file to the $team team"
Write-Host ""
Write-Host "Team can use it with:"
Write-Host "`$env:KUBECONFIG=`"${team}-kubeconfig.yaml`""
Write-Host "kubectl get pods"
```

### Testing Team Access

```powershell
# Set team kubeconfig
$env:KUBECONFIG = "margin-kubeconfig.yaml"

# Test access (should work)
kubectl get pods -n margin
kubectl create deployment test --image=nginx -n margin

# Test isolation (should fail)
kubectl get pods -n collateral
# Error: Forbidden ❌

kubectl create namespace hacker
# Error: Forbidden ❌
```

## Get Ingress Gateway Access

```bash
# For NodePort (K3s local)
export INGRESS_HOST=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[0].address}')
export INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-ingress -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')

echo "Ingress available at: http://$INGRESS_HOST:$INGRESS_PORT"

# For LoadBalancer (cloud)
kubectl get svc istio-ingressgateway -n istio-ingress
# Use EXTERNAL-IP from output
```

## Verify Team Access (RBAC Testing)

After deployment, test that teams can only access their own namespaces:

```powershell
# Test margin-sa permissions
kubectl auth can-i create pods -n margin --as=system:serviceaccount:margin:margin-sa
# Expected: yes ✅

kubectl auth can-i create pods -n collateral --as=system:serviceaccount:margin:margin-sa
# Expected: no ❌

# Test collateral-sa permissions
kubectl auth can-i create pods -n collateral --as=system:serviceaccount:collateral:collateral-sa
# Expected: yes ✅

kubectl auth can-i create pods -n margin --as=system:serviceaccount:collateral:collateral-sa
# Expected: no ❌

# Verify full namespace access
kubectl auth can-i "*" "*" -n margin --as=system:serviceaccount:margin:margin-sa
# Expected: yes ✅ (full access in their namespace)

# Verify NO cluster-wide access
kubectl auth can-i create namespaces --as=system:serviceaccount:margin:margin-sa
# Expected: no ❌ (cannot create cluster-level resources)
```

## Outputs

```bash
# View all outputs
terraform output

# Namespace outputs
terraform output created_namespaces       # Returns list of team namespaces
terraform output environment              # Returns: dev/staging/prod

# RBAC outputs
terraform output team_service_accounts    # Returns map of ServiceAccounts
terraform output rbac_roles_created       # Returns list of Roles
terraform output rbac_bindings_created    # Returns list of RoleBindings

# Istio outputs
terraform output istio_base_version
terraform output ingress_gateway_namespace
terraform output gateway_crd_registered   # Returns: true if CRDs ready
terraform output istio_ready_for_gateway  # Returns: true if ready for Gateway module
```

## Upgrading Istio

```bash
# Update version in terraform.tfvars
istio_version = "1.21.0"

# Apply changes
terraform plan
terraform apply
```

## Uninstalling Istio

⚠️ **WARNING**: This will remove Istio and affect all services using it!

```bash
terraform destroy
```

## Troubleshooting

### Pods not starting

```bash
# Check pod logs
kubectl logs -n istio-system -l app=istiod

# Check events
kubectl get events -n istio-system --sort-by='.lastTimestamp'
```

### Ingress Gateway not accessible

```bash
# Check service
kubectl get svc istio-ingressgateway -n istio-ingress

# Check pods
kubectl get pods -n istio-ingress

# Check logs
kubectl logs -n istio-ingress -l app=istio-ingressgateway
```

## Next Steps

After cluster setup:
1. ✅ Namespaces are created
2. ✅ Istio is installed
3. ✅ Gateway is deployed
4. Teams can now deploy their applications
5. See `../../02-team-setup/` for team deployment examples

## Dependency Chain

This module creates resources in the following order:

```
Step 0: team-namespaces (Create team namespaces)
    ↓ (no dependencies)
    |
    ├─→ Step 0.1: RBAC Resources (Parallel creation)
    |      ├─ ServiceAccounts (margin-sa, collateral-sa)
    |      ├─ Roles (namespace-admin)
    |      └─ RoleBindings (grant access)
    |   (depends_on: namespaces)
    |
    └─→ Step 1: istio-base (Install CRDs)
         ↓ (no dependencies on namespaces/RBAC)

Step 2: istiod (Install Control Plane)
    ↓ (depends_on: istio-base)

Step 3: istio-ingressgateway (Install Data Plane)
    ↓ (depends_on: istiod)

Step 4: CRD Verification (Verify Gateway CRD is ready)
    ↓ (depends_on: istio-base, istiod)
```

**Separate module:**
```
Step 5: shared-gateway (Create Gateway Resource)
    ↓ (run after this module completes)
    See: ../shared-gateway/
```

**Note:** 
- Namespaces and RBAC are created independently of Istio
- ServiceAccounts depend on namespaces
- Istio components can be created in parallel with RBAC


# =============================================================================
# Istio Service Mesh Installation — Base
# =============================================================================
# Installs Istio CRDs, control plane, and ingress gateway.
# Creates team namespaces and RBAC roles.
#
# Run this FIRST as cluster administrator, before shared-gateway/.
# =============================================================================

terraform {
  required_version = ">= 1.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 3.0"
    }
  }
}

# =============================================================================
# Kubernetes Provider Configuration
# =============================================================================
provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = var.kubeconfig_context
}

provider "helm" {
  kubernetes = {
    config_path    = var.kubeconfig_path
    config_context = var.kubeconfig_context
  }
}

# =============================================================================
# Namespace Creation
# =============================================================================
resource "kubernetes_namespace" "team_namespaces" {
  for_each = toset(var.team_namespaces)

  metadata {
    name = each.value

    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "app.kubernetes.io/part-of"    = "service-virtualization"
      "environment"                  = var.environment
    }
  }
}

# =============================================================================
# RBAC: namespace-admin Role in each team namespace
# =============================================================================
resource "kubernetes_role" "namespace_admin" {
  for_each = toset(var.team_namespaces)

  metadata {
    name      = "namespace-admin"
    namespace = each.value

    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
      "app.kubernetes.io/part-of"    = "service-virtualization"
    }
  }

  rule {
    api_groups = ["*"]
    resources  = ["*"]
    verbs      = ["*"]
  }

  depends_on = [kubernetes_namespace.team_namespaces]
}

# =============================================================================
# Istio Base (CRDs) — Step 1
# =============================================================================
resource "helm_release" "istio_base" {
  name       = "istio-base"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "base"
  version    = var.istio_version
  namespace  = "istio-system"

  create_namespace = true

  timeout = 600
  wait    = true
}

# =============================================================================
# Istiod (Control Plane) — Step 2
# =============================================================================
resource "helm_release" "istiod" {
  name       = "istiod"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "istiod"
  version    = var.istio_version
  namespace  = "istio-system"

  timeout = 600
  wait    = true

  depends_on = [helm_release.istio_base]
}

# =============================================================================
# Istio Ingress Gateway (NodePort for K3s) — Step 3
# =============================================================================
resource "helm_release" "istio_ingress" {
  count = var.install_ingress_gateway ? 1 : 0

  name       = "istio-ingressgateway"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "gateway"
  version    = var.istio_version
  namespace  = "istio-ingress"

  create_namespace = true

  # NodePort — K3s has no cloud load balancer.
  #
  # We explicitly list all service ports so we can add 8443 (HTTPS — HTTP over TLS).
  # Istio's gateway Helm chart defaults already include 15021, 80, and 443.
  # Port layout:
  #   80   → protocol HTTP  — plain HTTP (fallback / redirect)
  #   443  → protocol TLS   — TLS SIMPLE, inner TCP (IBM MQ, TIBCO)
  #   8443 → protocol HTTPS — TLS SIMPLE, inner HTTP (web consoles, APIs, UI)
  #
  # The Envoy proxy container automatically begins listening on port 8443 once
  # the shared Gateway resource (in shared-gateway/) is updated with the new
  # HTTPS server.  This service entry just exposes that container port externally
  # as a NodePort so traffic from outside the cluster can reach it.
  values = [
    yamlencode({
      service = {
        type = "NodePort"
        ports = [
          { name = "status-port", port = 15021, protocol = "TCP", targetPort = 15021 },
          { name = "http2",       port = 80,    protocol = "TCP", targetPort = 80 },
          { name = "tls",         port = 443,   protocol = "TCP", targetPort = 443 },
          { name = "https",       port = 8443,  protocol = "TCP", targetPort = 8443 },
        ]
      }
    })
  ]

  timeout = 900
  wait    = true

  depends_on = [helm_release.istiod]
}

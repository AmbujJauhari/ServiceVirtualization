# =============================================================================
# Shared Istio Gateway
# =============================================================================
# Creates the shared Gateway resource that all team VirtualServices reference.
#
# Run this AFTER base/ — the Istio CRDs must already be installed.
# =============================================================================

terraform {
  required_version = ">= 1.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
}

provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = var.kubeconfig_context
}

# =============================================================================
# Pre-flight: confirm the Gateway CRD is registered
# =============================================================================
# No depends_on needed here — by the time this module runs, base/ has already
# installed Istio and the CRD exists in the cluster.
data "kubernetes_resource" "gateway_crd" {
  api_version = "apiextensions.k8s.io/v1"
  kind        = "CustomResourceDefinition"

  metadata {
    name = "gateways.networking.istio.io"
  }
}

# =============================================================================
# Shared Gateway Resource
# =============================================================================
resource "kubernetes_manifest" "shared_gateway" {
  depends_on = [data.kubernetes_resource.gateway_crd]

  manifest = {
    apiVersion = "networking.istio.io/v1beta1"
    kind       = "Gateway"

    metadata = {
      name      = var.gateway_name
      namespace = var.gateway_namespace

      labels = {
        "app.kubernetes.io/managed-by" = "terraform"
      }

      annotations = {
        "description" = "Shared Gateway for ${join(", ", var.team_namespaces)} teams"
      }
    }

    spec = {
      selector = {
        istio = "ingressgateway"
      }

      servers = [
        {
          # Plain HTTP — no TLS. Used as a fallback/redirect port.
          port = {
            number   = var.gateway_http_port
            name     = "http"
            protocol = "HTTP"
          }
          hosts = var.gateway_hosts
        },
        {
          # TLS PASSTHROUGH (TCP) — Istio does NOT terminate TLS. It reads the SNI
          # hostname from the ClientHello to select a VirtualService, then forwards
          # the raw TLS bytes unchanged to the backend pod. The pod terminates TLS
          # end-to-end with its own certificate.
          #
          # Why PASSTHROUGH for IBM MQ / TIBCO EMS:
          #   These protocols embed SSL cipher negotiation inside the application
          #   protocol (MQ MQCD exchange). If Istio terminates TLS (SIMPLE mode)
          #   the application layer still negotiates SSL with the pod; since the pod
          #   channel has no SSLCIPH the connection fails with MQRC_SSL_NOT_ALLOWED.
          #   PASSTHROUGH lets TLS go end-to-end so the pod handles both the TLS
          #   handshake and the MQ SSL channel negotiation in one step.
          #
          # VirtualService routing for PASSTHROUGH uses `tls` route type with
          # `sniHosts` match (not `tcp` with `port` match). See team VS files.
          #
          # Use this port for raw TCP services: IBM MQ (1414), TIBCO EMS, etc.
          port = {
            number   = var.gateway_tls_port
            name     = "tls-passthrough"
            protocol = "TLS"
          }
          tls = {
            mode = "PASSTHROUGH"
          }
          hosts = var.gateway_hosts
        },
        {
          # HTTPS (HTTP over TLS) — Istio terminates TLS at the gateway using the
          # wildcard cert. After termination the inner protocol is HTTP, enabling
          # full HTTP routing: path-prefix, header matching, rewrites, retries, etc.
          #
          # Use this port for HTTP/HTTPS web services:
          #   - IBM MQ / ActiveMQ web consoles (port 9443)
          #   - Backend REST APIs
          #   - UI dashboards
          #
          # Key difference from tls-simple:
          #   protocol=TLS  → inner content treated as raw TCP  → TCP VirtualServices
          #   protocol=HTTPS → inner content treated as HTTP    → HTTP VirtualServices
          port = {
            number   = var.gateway_https_port
            name     = "https"
            protocol = "HTTPS"
          }
          tls = {
            mode           = "SIMPLE"
            credentialName = var.gateway_tls_credential
          }
          hosts = var.gateway_hosts
        }
      ]
    }
  }
}

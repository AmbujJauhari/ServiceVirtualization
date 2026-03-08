# =============================================================================
# Kubernetes Connection
# =============================================================================
# Use kubeconfig-local.yaml when running terraform from inside the VM (loopback)
# Use kubeconfig-bridge.yaml when running terraform from the host machine
kubeconfig_path    = "../../../kubeconfig-local.yaml"
kubeconfig_context = "k3s-local"

# =============================================================================
# Namespace Configuration
# =============================================================================
team_namespaces = ["margin", "collateral"]

# =============================================================================
# Gateway Configuration
# =============================================================================
gateway_name      = "shared-gateway"
gateway_namespace = "istio-ingress"
gateway_hosts     = ["*"]
gateway_http_port  = 80
gateway_tls_port   = 443   # protocol: TLS  — raw TCP after termination (IBM MQ, TIBCO)
gateway_https_port = 8443  # protocol: HTTPS — HTTP after termination  (consoles, APIs, UI)

# Name of the Kubernetes TLS Secret in the istio-ingress namespace.
# Create it AFTER base/ apply — the istio-ingress namespace is created by base/.
#
#   kubectl create secret tls gateway-tls-cert \
#     --cert=wildcard.crt \
#     --key=wildcard.key \
#     -n istio-ingress
gateway_tls_credential = "gateway-tls-cert"

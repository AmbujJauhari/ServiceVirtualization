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
environment     = "dev"

# =============================================================================
# Istio Configuration
# =============================================================================
istio_version           = "1.20.0"
install_ingress_gateway = true

# =============================================================================
# ServiceAccount Token Configuration
# =============================================================================
sa_token_secret_suffix = "token"

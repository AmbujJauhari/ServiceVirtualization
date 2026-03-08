# =============================================================================
# Istio Base — Variables
# =============================================================================

variable "kubeconfig_path" {
  description = "Path to the kubeconfig file for K3s cluster"
  type        = string
}

variable "kubeconfig_context" {
  description = "Kubernetes context to use from kubeconfig"
  type        = string
}

variable "team_namespaces" {
  description = "List of team namespaces to create"
  type        = list(string)
}

variable "environment" {
  description = "Environment label for namespaces (dev, staging, uat, prod)"
  type        = string
}

variable "istio_version" {
  description = "Istio version to install"
  type        = string
}

variable "install_ingress_gateway" {
  description = "Install Istio Ingress Gateway"
  type        = bool
}

variable "sa_token_secret_suffix" {
  description = "Suffix for ServiceAccount token Secret names"
  type        = string
}

# =============================================================================
# Shared Gateway — Variables
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
  description = "Team namespaces — used in the Gateway annotation"
  type        = list(string)
}

variable "gateway_name" {
  description = "Name of the shared Gateway resource"
  type        = string
}

variable "gateway_namespace" {
  description = "Namespace where the Gateway will be deployed"
  type        = string
}

variable "gateway_hosts" {
  description = "List of hosts the Gateway will accept (supports wildcards)"
  type        = list(string)
}

variable "gateway_http_port" {
  description = "Port for the plain HTTP server on the Gateway (typically 80)"
  type        = number
}

variable "gateway_tls_port" {
  description = "Port for TLS SIMPLE server on the Gateway — raw TCP after termination (IBM MQ, TIBCO, etc.)"
  type        = number
}

variable "gateway_https_port" {
  description = "Port for HTTPS server on the Gateway — HTTP after TLS termination (web console, backend API, UI)"
  type        = number
}

variable "gateway_tls_credential" {
  description = "Name of the Kubernetes TLS Secret in the istio-ingress namespace that holds the gateway's wildcard certificate and private key"
  type        = string
}

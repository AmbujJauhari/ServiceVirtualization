# =============================================================================
# Istio Base — Outputs
# =============================================================================

output "created_namespaces" {
  description = "List of created team namespaces"
  value       = [for ns in kubernetes_namespace.team_namespaces : ns.metadata[0].name]
}

output "environment" {
  description = "Environment label applied to namespaces"
  value       = var.environment
}

output "rbac_roles_created" {
  description = "RBAC roles created for teams"
  value       = [for role in kubernetes_role.namespace_admin : "${role.metadata[0].namespace}/${role.metadata[0].name}"]
}

output "istio_base_version" {
  description = "Installed Istio base version"
  value       = helm_release.istio_base.version
}

output "istiod_version" {
  description = "Installed Istiod version"
  value       = helm_release.istiod.version
}

output "istio_namespace" {
  description = "Istio system namespace"
  value       = helm_release.istio_base.namespace
}

output "ingress_gateway_installed" {
  description = "Whether Istio Ingress Gateway was installed"
  value       = var.install_ingress_gateway
}

output "ingress_gateway_namespace" {
  description = "Istio Ingress Gateway namespace"
  value       = var.install_ingress_gateway ? helm_release.istio_ingress[0].namespace : null
}

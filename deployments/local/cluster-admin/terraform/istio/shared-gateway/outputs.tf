# =============================================================================
# Shared Gateway — Outputs
# =============================================================================

output "gateway_crd_registered" {
  description = "Confirms Gateway CRD is registered and ready for use"
  value       = data.kubernetes_resource.gateway_crd.object != null
}

output "gateway_crd_name" {
  description = "Name of the registered Gateway CRD"
  value       = data.kubernetes_resource.gateway_crd.object.metadata.name
}

output "gateway_name" {
  description = "Name of the created Gateway resource"
  value       = kubernetes_manifest.shared_gateway.manifest.metadata.name
}

output "gateway_namespace" {
  description = "Namespace where the Gateway is deployed"
  value       = kubernetes_manifest.shared_gateway.manifest.metadata.namespace
}

output "gateway_reference" {
  description = "Full reference for teams to use in VirtualServices (namespace/name)"
  value       = "${kubernetes_manifest.shared_gateway.manifest.metadata.namespace}/${kubernetes_manifest.shared_gateway.manifest.metadata.name}"
}

output "gateway_hosts" {
  description = "Hosts accepted by the Gateway"
  value       = var.gateway_hosts
}

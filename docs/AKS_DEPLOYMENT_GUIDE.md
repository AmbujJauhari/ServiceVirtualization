# AKS Deployment Guide

This guide covers deploying the Service Virtualization platform to Azure Kubernetes Service (AKS) and private clusters. It follows the same three-layer architecture as the local deployment but uses Azure-native tooling for cluster provisioning, certificate management, and image registry.

> **Status**: Work in progress. Section headings are placeholders — content will be added as the AKS deployment is implemented and validated.

---

## Prerequisites

---

## Cluster Setup

### AKS cluster provisioning

### Node pool configuration

### Networking mode (CNI, kubenet)

---

## Container Registry

### Azure Container Registry (ACR) setup

### Image push commands

### Attaching ACR to AKS (`az aks update --attach-acr`)

### Image pull secrets

---

## Istio Installation on AKS

### Installing via Helm (same as local)

### LoadBalancer vs NodePort service type

### Static IP for Istio Ingress Gateway

---

## Namespace and RBAC Setup

### Creating team namespaces

### ServiceAccounts and RoleBindings

---

## TLS Certificates

### Using Azure Key Vault certificates

### Using cert-manager with Let's Encrypt

### Wildcard certificate strategy (same as local)

### Loading secrets into Kubernetes namespaces

---

## DNS Configuration

### Azure DNS zones

### Private DNS zones for private clusters

### Mapping Istio Gateway IP to `*.service-virtualization.local` (or custom domain)

---

## Deploy Optional Services

### IBM MQ

### Tibco EMS

### Apache Kafka

### ActiveMQ

---

## Deploy Core Product (Backend and UI)

### Backend values for AKS (image tags, registry)

### UI values for AKS

### Backend and UI together

---

## Istio VirtualService and Gateway Configuration

### Shared Gateway with real TLS certificate

### HTTP, TLS PASSTHROUGH, and HTTPS listeners

### Per-team VirtualServices

---

## Verification

### Smoke tests

### Integration test suite

---

## Maintenance

### Upgrading a Helm release

### Rotating TLS certificates

### Scaling services

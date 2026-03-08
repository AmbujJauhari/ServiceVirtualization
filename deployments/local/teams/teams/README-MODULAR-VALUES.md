# Modular Helm Values Structure

## Overview

Team values are organized into modular, self-contained files to support flexible deployment patterns across multiple Helm repositories.

## Directory Structure

```
teams/
├── collateral/
│   ├── core-product/
│   │   ├── backend-values.yaml      # Backend service only
│   │   └── ui-values.yaml           # UI service only
│   ├── optional-services/
│   │   ├── ibmmq-values.yaml        # IBM MQ instances only
│   │   ├── kafka-values.yaml        # Kafka instances only
│   │   ├── tibco-values.yaml        # TIBCO EMS instances only
│   │   └── activemq-values.yaml     # ActiveMQ instances only
│   └── networking/
│       └── istio/
│           └── virtualservices.yaml # Istio VirtualService
│
└── margin/
    ├── core-product/
    │   ├── backend-values.yaml
    │   └── ui-values.yaml
    ├── optional-services/
    │   ├── ibmmq-values.yaml
    │   ├── kafka-values.yaml
    │   ├── tibco-values.yaml
    │   └── activemq-values.yaml
    └── networking/
        └── istio/
            └── virtualservices.yaml
```

## Design Principles

1. **Self-Contained**: Each values file includes all necessary configuration (global, serviceAccount, service-specific)
2. **Zero Dependencies**: No external file references or includes
3. **Multi-Repo Ready**: Can be used independently across different Helm repositories
4. **Enable/Disable Pattern**: Each file explicitly enables its service and disables others

## Usage Examples

### Deploy Single Service

**Backend Only:**
```bash
helm install margin-backend ./helm-charts/service-virtualization \
  -f teams/margin/core-product/backend-values.yaml \
  --namespace margin
```

**IBM MQ Only:**
```bash
helm install margin-ibmmq ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/ibmmq-values.yaml \
  --namespace margin
```

**Kafka Only:**
```bash
helm install collateral-kafka ./helm-charts/service-virtualization \
  -f teams/collateral/optional-services/kafka-values.yaml \
  --namespace collateral
```

### Deploy Multiple Services

**Backend + UI:**
```bash
helm install margin-core ./helm-charts/service-virtualization \
  -f teams/margin/core-product/backend-values.yaml \
  -f teams/margin/core-product/ui-values.yaml \
  --namespace margin
```

**All IBM MQ + Kafka:**
```bash
helm install margin-messaging ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/ibmmq-values.yaml \
  -f teams/margin/optional-services/kafka-values.yaml \
  --namespace margin
```

### Deploy Full Stack

**All Services (Core + Optional):**
```bash
helm install collateral-full ./helm-charts/service-virtualization \
  -f teams/collateral/core-product/backend-values.yaml \
  -f teams/collateral/core-product/ui-values.yaml \
  -f teams/collateral/optional-services/ibmmq-values.yaml \
  -f teams/collateral/optional-services/kafka-values.yaml \
  -f teams/collateral/optional-services/tibco-values.yaml \
  -f teams/collateral/optional-services/activemq-values.yaml \
  --namespace collateral
```

## Configuration Duplication

Each values file duplicates the following sections for independence:

- `team` - Team name and namespace
- `global` - Image registry, pull secrets, security context
- `serviceAccount` - SA name, annotations, role binding

**This is intentional** to ensure each file can be used standalone across different repositories.

## Creating Merged Values (Optional)

If you prefer a single monolithic file for full deployments, create a merge script:

### PowerShell Example
```powershell
# teams/collateral/merge-values.ps1
$files = @(
    "core-product/backend-values.yaml",
    "core-product/ui-values.yaml",
    "optional-services/ibmmq-values.yaml",
    "optional-services/kafka-values.yaml",
    "optional-services/tibco-values.yaml",
    "optional-services/activemq-values.yaml"
)

# Requires yq tool
yq eval-all '. as $item ireduce ({}; . * $item)' $files > app-values.yaml
```

### Bash Example
```bash
#!/bin/bash
# teams/margin/merge-values.sh
yq eval-all '. as $item ireduce ({}; . * $item)' \
  core-product/backend-values.yaml \
  core-product/ui-values.yaml \
  optional-services/ibmmq-values.yaml \
  optional-services/kafka-values.yaml \
  optional-services/tibco-values.yaml \
  optional-services/activemq-values.yaml \
  > app-values.yaml
```

## Maintenance

When updating configurations:

1. Update the relevant modular file(s)
2. Test deployment with `helm template` command
3. If using a merged values file, regenerate it

## Helm Template Testing

Validate values without deploying:

```bash
# Test single service
helm template test-margin ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/kafka-values.yaml \
  --namespace margin

# Test multiple services
helm template test-collateral ./helm-charts/service-virtualization \
  -f teams/collateral/core-product/backend-values.yaml \
  -f teams/collateral/optional-services/ibmmq-values.yaml \
  --namespace collateral
```

## Benefits

✅ **Flexibility**: Deploy any combination of services
✅ **Multi-Repo**: Use same values across different Helm repositories
✅ **No Hidden Dependencies**: All config is explicit in each file
✅ **CI/CD Friendly**: Easy to integrate into automated pipelines
✅ **Team Autonomy**: Teams control their own service deployments

## Migration from Monolithic Values

The old monolithic `app-values.yaml` files have been removed. To deploy the full stack, use multiple `-f` flags as shown in the examples above, or create your own merge script.

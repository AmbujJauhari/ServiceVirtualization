# Architecture Decision Records

This document captures the significant architectural decisions made during the design and implementation of the Service Virtualization platform. Each record describes the context that forced a decision, the decision itself, and the consequences that follow from it.

ADRs are numbered sequentially and grouped by concern. Once accepted, a decision is not deleted — if it is reversed, a new ADR is added that supersedes it.

---

## Table of Contents

**Deployment Architecture**
- [ADR-001](#adr-001-three-layer-deployment-separation) Three-layer deployment separation
- [ADR-002](#adr-002-shared-istio-gateway-with-three-protocol-ports) Shared Istio Gateway with three protocol ports
- [ADR-003](#adr-003-terraform-split-into-two-independent-modules) Terraform split into two independent modules
- [ADR-004](#adr-004-rbac-as-a-separate-helm-chart) RBAC as a separate Helm chart

**Helm Chart Design**
- [ADR-005](#adr-005-single-helm-chart-for-all-services-with-conditional-rendering) Single chart for all services with conditional rendering
- [ADR-006](#adr-006-map-based-instances-over-list-based-instances) Map-based instances over list-based instances
- [ADR-007](#adr-007-one-values-file-per-instance-combined-with-multiple--f-flags) One values file per instance, combined with multiple `-f` flags
- [ADR-008](#adr-008-configdefaultenabled-pattern-for-auto-generated-service-configuration) `config.default.enabled` pattern for auto-generated service configuration

**IBM MQ**
- [ADR-009](#adr-009-tls-passthrough-for-the-ibm-mq-wire-protocol) TLS PASSTHROUGH for the IBM MQ wire protocol
- [ADR-010](#adr-010-sni-derived-from-channel-name-for-ibm-mq-routing) SNI derived from channel name for IBM MQ routing
- [ADR-011](#adr-011-separate-https-port-and-destinationrule-for-the-ibm-mq-web-console) Separate HTTPS port and DestinationRule for the IBM MQ web console
- [ADR-012](#adr-012-chlauth-and-mcauser-configuration-to-fix-mqrc_not_authorized-2035) CHLAUTH and MCAUSER configuration to fix MQRC_NOT_AUTHORIZED (2035)

**Tibco EMS**
- [ADR-013](#adr-013-dual-listeners-tcp-7222-and-ssl-7243-for-tibco-ems) Dual listeners TCP 7222 and SSL 7243 for Tibco EMS
- [ADR-014](#adr-014-no-destinationrule-for-tibco-contrast-with-ibm-mq) No DestinationRule for Tibco
- [ADR-015](#adr-015-hostname-verification-disabled-for-backend-to-tibco-internal-connections) Hostname verification disabled for backend-to-Tibco internal connections

**Apache Kafka**
- [ADR-016](#adr-016-kraft-mode-no-zookeeper) KRaft mode — no Zookeeper
- [ADR-017](#adr-017-confluent-cp-kafka-image-over-plain-apache-kafka) Confluent `cp-kafka` image over plain Apache Kafka
- [ADR-018](#adr-018-backend-connects-to-kafka-via-plaintext-port-9092-not-ssl) Backend connects to Kafka via PLAINTEXT port 9092, not SSL

**Backend**
- [ADR-019](#adr-019-registry-based-multi-server-configuration-via-environment-variables) Registry-based multi-server configuration via environment variables
- [ADR-020](#adr-020-jks-truststore-built-at-pod-startup-via-init-container) JKS truststore built at pod startup via init container

**UI**
- [ADR-021](#adr-021-runtime-path-prefix-detection-no-build-time-environment-variables) Runtime path-prefix detection — no build-time environment variables

---

## Deployment Architecture

---

### ADR-001: Three-Layer Deployment Separation

**Status:** Accepted

**Context**

The platform needs to run in environments managed by different people with different concerns and different permission levels. A cluster administrator provisions infrastructure. A product team maintains the Helm chart. Application teams deploy their own services into their own namespaces. If a single Helm chart or a single repository owned all of this, any change — even a values file tweak by a team — would require access to infrastructure-level resources. Teams would block each other.

Additionally, the platform needs to be infrastructure-agnostic: it should be deployable on local K3s, AKS, EKS, or any Kubernetes cluster without changing the product chart.

**Decision**

The platform is split into three independent layers, each with a distinct owner and lifecycle:

| Layer | Location | Owner | Lifecycle |
|---|---|---|---|
| Cluster Admin | `deployments/local/cluster-admin/` | Infrastructure team | Once per environment |
| Product (Helm chart) | `helm-charts/service-virtualization/` | Product team | Per release |
| Team deployment | `deployments/local/teams/teams/<team>/` | Application team | Per service change |

The product Helm chart has no knowledge of Ingress, Istio, DNS, or RBAC. It renders Deployments and Services only. The cluster admin layer installs Istio and creates namespaces. Each team brings their own values files, VirtualServices, and RBAC chart.

**Consequences**

- Teams can deploy and upgrade their services without touching infrastructure
- The product chart can be tested in any Kubernetes environment without Istio
- Adding a new team requires no changes to the product chart — only a new folder under `teams/`
- Infrastructure changes (e.g., upgrading Istio) do not require product chart changes
- The split requires discipline: teams must apply both their values files and their VirtualServices; forgetting either will result in a deployed service with no external routing

---

### ADR-002: Shared Istio Gateway with Three Protocol Ports

**Status:** Accepted

**Context**

The platform serves three fundamentally different traffic types simultaneously:

1. Plain HTTP — for the UI and backend REST API
2. Raw TCP with TLS (not HTTP) — for IBM MQ, Tibco EMS, and Kafka wire protocols. These protocols are not HTTP; they cannot be terminated and re-proxied as HTTP.
3. HTTPS — for the IBM MQ web console, which is HTTP-over-TLS and needs path-based routing after TLS termination

Without a shared gateway, each team would need either separate LoadBalancer services (expensive, one IP per protocol) or complex per-service NodePort management.

**Decision**

A single shared `Gateway` resource (in `istio-ingress` namespace) exposes three ports with different TLS modes:

| Port | Istio protocol | TLS mode | Purpose |
|---|---|---|---|
| 80 | HTTP | None | UI, Backend REST API |
| 443 | TLS | PASSTHROUGH | IBM MQ, Tibco EMS, Kafka (wire protocols) |
| 8443 | HTTPS | SIMPLE (termination) | IBM MQ web console |

`gateway_hosts = ["*"]` in Terraform so the gateway accepts all hostnames — teams add VirtualServices with the specific `sniHosts` or path rules they need without modifying the gateway.

**Consequences**

- All teams and all protocols share one Ingress IP/NodePort — no per-team LoadBalancer needed
- Port 443 PASSTHROUGH and port 8443 HTTPS termination are structurally incompatible: services that need HTTP routing after TLS (consoles, APIs) cannot use port 443, and services with non-HTTP wire protocols cannot use port 8443
- New teams add VirtualServices pointing to the same gateway — no gateway changes required
- The gateway TLS credential (`gateway-tls-cert`) must be valid for all hostnames that use port 8443, which is satisfied by the wildcard certificate

---

### ADR-003: Terraform Split into Two Independent Modules

**Status:** Accepted

**Context**

The Istio installation and the shared `Gateway` resource creation are logically related but have a hard dependency: the `Gateway` CRD only exists after Istiod is installed and running. Terraform's `kubernetes_manifest` resource validates the manifest against the live cluster API at `terraform plan` time — not at apply time. If both resources were in a single module, `terraform plan` would fail when the CRD does not yet exist, even with a `depends_on`.

**Decision**

Terraform is split into two independent modules under `cluster-admin/terraform/istio/`:

| Module | What it creates |
|---|---|
| `base/` | Team namespaces, RBAC roles, Istio CRDs, Istiod, Ingress Gateway |
| `shared-gateway/` | The `Gateway` Kubernetes manifest (requires the CRD from `base/` to exist) |

Each module is a separate `terraform apply`. The operator runs `base/` first, then `shared-gateway/`. The ordering is enforced naturally by the fact that `shared-gateway/` will fail at plan time if `base/` has not been applied.

**Consequences**

- `terraform apply` must be run twice in sequence — a minor operational overhead documented in the deployment guide
- There is no `depends_on` between the modules, so the operator must know the correct order
- Splitting prevents the more serious problem of `terraform plan` being permanently broken in a fresh environment
- Each module can be independently destroyed and re-applied (useful for Istio upgrades)

---

### ADR-004: RBAC as a Separate Helm Chart

**Status:** Accepted

**Context**

The product Helm chart needs a `ServiceAccount` name to set on pods so that Kubernetes can enforce pod-level identity. Creating the `ServiceAccount` and `RoleBinding` inside the product chart would mean the product chart requires cluster-level permissions to manage RBAC resources. Application teams typically do not have those permissions — only a cluster admin does.

If RBAC were inside the product chart, a team could not install the chart themselves without elevated permissions, defeating the three-layer separation goal.

**Decision**

RBAC resources (`ServiceAccount` and `RoleBinding`) are managed as templates directly inside each team's umbrella chart (`deployments/local/teams/teams/<team>/umbrella/templates/rbac.yaml`). They are rendered on the first `helm upgrade --install` and are idempotent on every subsequent upgrade. The product chart (`sv` subchart) reads the `ServiceAccount` name from `sv-global.yaml` via `sv.serviceAccount.name` and only references it — it never creates RBAC resources itself.

**Consequences**

- RBAC is deployed as part of the normal team upgrade workflow — no separate cluster-admin step required
- Teams need namespace-scoped RBAC write permissions (create ServiceAccount, RoleBinding), which is standard for teams managing their own namespace
- `ServiceAccount` name is defined once in `umbrella/values/sv-global.yaml` and shared between the RBAC template and the product subchart

---

## Helm Chart Design

---

### ADR-005: Single Helm Chart for All Services with Conditional Rendering

**Status:** Accepted

**Context**

The platform manages five distinct service types: IBM MQ, Tibco EMS, Kafka, ActiveMQ, and the core product (backend + UI). Each could be a separate Helm chart. However, teams need to select which services they deploy; forcing them to install five charts separately adds operational overhead and makes it harder to share cross-cutting configuration (security context, image pull secrets, ServiceAccount).

**Decision**

A single Helm chart (`helm-charts/service-virtualization/`) renders all service types. Each service type is gated by an `enabled` flag:

```yaml
ibmmq:
  enabled: true   # renders ibmmq.yaml templates
tibco:
  enabled: false  # tibco.yaml templates are skipped entirely
```

Cross-cutting concerns (`global.securityContext`, `global.imagePullSecrets`, `serviceAccount.name`) are defined once in `sv-global.yaml` and apply to every service rendered by the chart.

**Consequences**

- A team deploys all their services in a single Helm release, or splits into separate releases by passing different values files — both patterns work
- The chart is larger and more complex than per-service charts, but the complexity is in templating logic, not in the interfaces exposed to teams
- Adding a new protocol requires adding a template file to the chart — it does not require teams to install a new chart
- A team cannot partially upgrade one service within a combined release without templating awareness; separate releases per service type (e.g., `margin-ibmmq`, `margin-tibco`) avoid this concern and are the recommended pattern for production

---

### ADR-006: Map-Based Instances Over List-Based Instances

**Status:** Accepted

**Context**

Each team needs multiple instances of the same service type (e.g., IBM MQ ICG and IBM MQ RTO). The natural representation in YAML is either a list or a map:

```yaml
# List (rejected)
instances:
  - name: icg
    ...
  - name: rto
    ...

# Map (chosen)
instances:
  icg:
    ...
  rto:
    ...
```

Helm merges values files by key path. When two `-f` files both define a list under the same key, the **last file wins entirely** — the list from the first file is discarded. When two files both define a map key, Helm **merges them** — keys from both files are present in the result.

This is the critical distinction: with a list, passing `-f ibmmq-icg-values.yaml -f ibmmq-rto-values.yaml` would result in only the RTO instance being deployed. With a map, both instances are present after the merge.

**Decision**

All multi-instance services use a map keyed by instance name:

```yaml
ibmmq:
  instances:
    icg:   { ... }   # ibmmq-icg-values.yaml defines this key
    rto:   { ... }   # ibmmq-rto-values.yaml defines this key
```

Helm iterates the map with `range $name, $instance := .Values.ibmmq.instances` in the template, making all resource names deterministic from the key.

**Consequences**

- Instance names are part of all Kubernetes resource names — they cannot contain uppercase letters or special characters (Helm `| lower` or `| trim` is applied)
- The map key must be unique per release — deploying the same instance name twice would silently overwrite the first
- Teams adding a new instance to an existing release do so by adding a new map key in a new values file and passing it with an additional `-f` flag on `helm upgrade` — no existing values files are modified

---

### ADR-007: One Values File per Instance, Combined with Multiple `-f` Flags

**Status:** Accepted

**Context**

Teams may need to deploy instances independently. The collateral team may deploy only ICG on day one and add RTO ten days later. The margin team may want to deploy both together from the start. If all instances lived in a single values file per service type, adding a new instance would require modifying the existing file — increasing merge conflict risk in version-controlled deployments.

**Decision**

Each instance has its own dedicated values file inside the umbrella chart's `values/` directory:

```
umbrella/values/
  ibmmq-icg.yaml   → sv.ibmmq.instances.icg: { ... } + networking.ibmmq.instances.icg: { ... }
  ibmmq-rto.yaml   → sv.ibmmq.instances.rto: { ... } + networking.ibmmq.instances.rto: { ... }
```

Teams combine instances by passing multiple `-f` flags to `helm upgrade --install` on the umbrella chart:

```powershell
$U = "deployments/local/teams/teams"

# Deploy both together (margin)
helm upgrade --install margin $U/margin/umbrella \
  -f $U/margin/umbrella/values/sv-global.yaml \
  -f $U/margin/umbrella/values/ibmmq-icg.yaml \
  -f $U/margin/umbrella/values/ibmmq-rto.yaml \
  --namespace margin

# Deploy ICG only — omit the second -f flag (collateral day one)
helm upgrade --install collateral $U/collateral/umbrella \
  -f $U/collateral/umbrella/values/sv-global.yaml \
  -f $U/collateral/umbrella/values/ibmmq-icg.yaml \
  --namespace collateral
```

Adding RTO to an existing ICG-only release is a `helm upgrade` with the second `-f` flag added — no file edits. Each values file also contains the matching `networking.ibmmq.instances.*` block so the Istio VirtualService and DestinationRule for that instance are created in the same upgrade.

**Consequences**

- Helm merge of maps (ADR-006) is what makes this work — lists would not support this pattern
- The operator must remember which `-f` files were passed to the original install when running `helm upgrade` — all previously installed instances must be included or they will be removed from the release
- This is typically managed with a CI/CD pipeline or a script that always passes all known `-f` flags for a team's full set of instances

---

### ADR-008: `config.default.enabled` Pattern for Auto-Generated Service Configuration

**Status:** Accepted

**Context**

IBM MQ requires MQSC scripts to configure queue managers, channels, queues, and authentication. Tibco EMS requires `tibemsd.conf`. Writing these configuration files correctly from scratch is non-trivial and error-prone for teams that just want a working service. At the same time, teams that use this platform as a drop-in replacement for existing services need to bring their own custom configuration to match the real service's topology.

**Decision**

Every service that requires configuration files uses a two-path pattern controlled by `config.default.enabled`:

- `config.default.enabled: true` — the Helm template auto-generates the configuration from structured values (queue names, channel names, auth passwords). This is the path for new teams or local development.
- `config.default.enabled: false` — `config.custom.<content>` is required and is rendered verbatim into the ConfigMap. This is the path for teams that need exact parity with a real service.

The chart enforces this at render time with `fail()`:

```yaml
{{- if not .config.default.enabled }}
  {{- if or (not .config.custom.mqsc) (eq (trim .config.custom.mqsc) "") }}
  {{- fail "config.custom.mqsc is REQUIRED when config.default.enabled is false" }}
  {{- end }}
{{- end }}
```

**Consequences**

- Teams cannot accidentally deploy with no configuration — the chart fails at `helm template` time with a clear error message if neither path is satisfied
- The default path makes onboarding fast; the custom path preserves full flexibility
- When `default.enabled: true`, the generated MQSC is deterministic from the values — teams can inspect it with `helm template` before deploying
- Custom MQSC is embedded as a literal string in the values file, which means it is version-controlled alongside the deployment configuration

---

## IBM MQ

---

### ADR-009: TLS PASSTHROUGH for the IBM MQ Wire Protocol

**Status:** Accepted

**Context**

IBM MQ uses a proprietary binary wire protocol (MQ Channel Definition, MQCD) over TCP. It is not HTTP. The Istio Gateway cannot parse, terminate, and re-proxy this protocol as it would for HTTPS. If Istio were to terminate TLS and try to speak the MQ protocol as a proxy, the connection would fail — Istio's Envoy proxy does not understand MQCD.

Additionally, the IBM MQ client begins the connection with a plain MQCD handshake *before* starting TLS. Envoy's TLS Inspector buffers these pre-TLS bytes while waiting for the TLS `ClientHello`. Once the `ClientHello` arrives, Envoy extracts the SNI, selects the matching filter chain, and forwards the entire buffered stream (MQCD bytes + TLS) to the destination pod, where IBM MQ handles the full handshake.

**Decision**

The Istio Gateway port 443 is configured with `protocol: TLS` and `tls.mode: PASSTHROUGH`. Envoy reads the SNI from the `ClientHello` to select the destination but does not decrypt the stream. The IBM MQ pod receives the complete TCP stream and handles TLS itself using the certificate placed in `/etc/mqm/pki/keys/default/` by the init container.

```yaml
# shared-gateway — port 443
servers:
  - port:
      number: 443
      protocol: TLS
    tls:
      mode: PASSTHROUGH
    hosts: ["*"]
```

**Consequences**

- The IBM MQ pod must hold a valid TLS certificate — the gateway cannot provide one on its behalf
- TLS configuration is per-instance (each IBM MQ pod holds the wildcard cert)
- Istio cannot inspect, filter, or apply policies to the MQ traffic payload — only connection-level policies (which destination) are possible
- The pre-TLS MQCD buffering by Envoy's TLS Inspector is a known behaviour and works correctly; it is not a concern for production use

---

### ADR-010: SNI Derived from Channel Name for IBM MQ Routing

**Status:** Accepted

**Context**

Multiple IBM MQ instances (ICG, RTO, and potentially more) must all be accessible on the same port 443. Istio's TLS PASSTHROUGH routing uses SNI hostname matching to select the destination. The SNI hostname must be unique per instance.

The IBM MQ Java client (`com.ibm.mq.allclient`) derives the TLS SNI hostname automatically from the channel name using a fixed formula:

```
lowercase(channelName)
replace each '.' with '2e-'
append '.chl.mq.ibm.com'

Example: MARGIN.ICG.SVRCONN → margin2e-icg2e-svrconn.chl.mq.ibm.com
```

The client sets this derived value as the SNI hostname during the TLS handshake. There is no API to override it — the SNI is always channel-name-derived.

**Decision**

Each IBM MQ instance has a unique channel name following the convention `<TEAM>.<INSTANCE>.SVRCONN`. The Istio VirtualService `sniHosts` is set to the derived hostname:

```yaml
# ibmmq-icg.yaml
spec:
  hosts:
  - "margin2e-icg2e-svrconn.chl.mq.ibm.com"
  tls:
  - match:
    - sniHosts: ["margin2e-icg2e-svrconn.chl.mq.ibm.com"]
    route:
    - destination:
        host: margin-ibmmq-mq-icg.margin.svc.cluster.local
        port:
          number: 1414
```

Generic channel names like `DEV.APP.SVRCONN` must not be reused across instances — they would produce colliding SNI values.

**Consequences**

- Channel name uniqueness is a hard deployment constraint — two instances with the same channel name cannot coexist on port 443
- The naming convention `<TEAM>.<INSTANCE>.SVRCONN` guarantees uniqueness across teams and instances
- Test clients must use the channel name configured in the MQSC, not a generic one — otherwise Istio routes to the wrong pod
- The `.chl.mq.ibm.com` domain does not need to be a real resolvable DNS name — Istio reads SNI from the TLS handshake before any DNS lookup

---

### ADR-011: Separate HTTPS Port and DestinationRule for the IBM MQ Web Console

**Status:** Accepted

**Context**

IBM MQ includes a web-based management console served over HTTPS on pod port 9443. This is standard HTTP-over-TLS, not the MQ wire protocol. It requires path-based routing to reach the correct instance (`/margin/mq-icg/ibmmq/console/`). Path-based routing requires TLS termination at the gateway — but port 443 uses TLS PASSTHROUGH (ADR-009), which makes path-based routing impossible on that port.

Furthermore, the IBM MQ pod's console port (9443) is itself HTTPS — it does not accept plain HTTP. After Istio terminates the client's TLS on port 8443, it must re-originate TLS toward the pod. Without a `DestinationRule`, Istio would attempt plain HTTP to port 9443 and be rejected.

**Decision**

The IBM MQ console uses a dedicated port 8443 on the shared gateway (`protocol: HTTPS`, `mode: SIMPLE`) where Istio terminates the client's TLS. A `DestinationRule` with `mode: SIMPLE` and `insecureSkipVerify: true` tells Istio to re-originate TLS toward port 9443 on the pod. The `insecureSkipVerify` flag is needed because the pod's internal certificate (same wildcard cert) is self-signed and does not match `margin-ibmmq-mq-icg.margin.svc.cluster.local`.

The DestinationRule uses `portLevelSettings` scoped to port 9443 only, so the MQ wire protocol on port 1414 is unaffected:

```yaml
spec:
  host: margin-ibmmq-mq-icg.margin.svc.cluster.local
  trafficPolicy:
    portLevelSettings:
    - port:
        number: 9443
      tls:
        mode: SIMPLE
        insecureSkipVerify: true
```

**Consequences**

- IBM MQ is the only service in this platform that requires both a TLS PASSTHROUGH route (wire protocol) and an HTTPS termination route (console), hence it is the only service with a DestinationRule
- Two VirtualServices exist per IBM MQ instance: one for the wire protocol (TLS type, sniHosts match), one for the console (HTTP type, hostname match)
- Tibco and Kafka do not have web consoles requiring this treatment and therefore have no DestinationRules (see ADR-014)

---

### ADR-012: CHLAUTH and MCAUSER Configuration to Fix MQRC_NOT_AUTHORIZED (2035)

**Status:** Accepted

**Context**

IBM MQ's default security model uses Channel Authentication Records (CHLAUTH) that block any connection where the OS username presented by the client does not match a permitted user on the queue manager host. In a containerised environment, the Java client sends the OS username of the process running the JVM (e.g., a service account name or an arbitrary UID string). The queue manager, running in its own pod with its own user space, has no record of that username, and CHLAUTH rejects the connection with reason code 2035 (`MQRC_NOT_AUTHORIZED`).

Additionally, IBM MQ's `CONNAUTH` by default checks the queue manager's OS password file for the connecting user. In containers, there is no shared OS user database between the client and the server, so CONNAUTH authentication always fails.

**Decision**

The auto-generated MQSC (when `config.default.enabled: true`) includes the following for every configured channel:

```mqsc
DEFINE CHANNEL('MARGIN.ICG.SVRCONN') CHLTYPE(SVRCONN) MCAUSER('app') ...
SET CHLAUTH('MARGIN.ICG.SVRCONN') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(CHANNEL) CHCKCLNT(ASQMGR)
ALTER AUTHINFO('DEV.AUTHINFO') AUTHTYPE(IDPWOS) CHCKCLNT(NONE) ADOPTCTX(NO)
REFRESH SECURITY(*) TYPE(CONNAUTH)
```

`MCAUSER('app')` overrides the client-supplied username with a fixed value that exists in the queue manager's user space. `CHCKCLNT(NONE)` disables OS password verification entirely. `ADOPTCTX(NO)` prevents the queue manager from adopting the client's security context.

**Consequences**

- Any client that can reach port 1414 (through Istio's TLS check) can connect without providing credentials — authentication is effectively delegated to the TLS layer (possession of the correct certificate, validated by `SSLCAUTH(OPTIONAL)`)
- This configuration is appropriate for a non-production service virtualization platform where network-level security (Istio, TLS) is the primary control
- For production IBM MQ deployments, `CONNAUTH` should be re-enabled with proper user management — this configuration must not be used as a template for real IBM MQ instances

---

## Tibco EMS

---

### ADR-013: Dual Listeners TCP 7222 and SSL 7243 for Tibco EMS

**Status:** Accepted

**Context**

The backend service (running inside the same Kubernetes cluster) connects to Tibco EMS to dispatch stub responses. The backend pod can reach Tibco directly via the ClusterIP service without going through Istio. External clients (test applications, CI jobs) connect through Istio's TLS PASSTHROUGH on port 443.

If Tibco only exposed a single SSL listener, the backend would need to perform full TLS with hostname verification, and the internal connection URL would use an external hostname that doesn't resolve inside the cluster (the same advertised-listener problem described for Kafka in ADR-018). If Tibco only exposed plain TCP, external clients would have no TLS protection.

**Decision**

Each Tibco EMS instance is configured with dual listeners in `tibemsd.conf`:

```
listen = tcp://0.0.0.0:7222    # internal — backend connects here without TLS
listen = ssl://0.0.0.0:7243    # external — Istio TLS PASSTHROUGH routes here
```

The Kubernetes Service exposes both ports. The backend connects via `tcp://`:

```yaml
TIBCO_REGISTRY_SERVERA_URL: "ssl://margin-tibco-service-virtualization-tibco-serverA.margin.svc.cluster.local:7222"
```

External clients connect via `ssl://servera.tibco.margin.service-virtualization.local:443`, which Istio routes to pod port 7243.

**Consequences**

- The plain TCP listener (7222) is not exposed outside the cluster (ClusterIP service, no external VirtualService for that port) — it is only reachable by pods in the same namespace
- The SSL listener (7243) requires the Tibco pod to hold a valid TLS certificate, provided by the init container from the shared team TLS secret
- Clients must choose the correct URL scheme (`tcp://` for internal, `ssl://` for external) — using `ssl://` for an internal connection (ADR-015 explains why this causes hostname verification failures)

---

### ADR-014: No DestinationRule for Tibco (Contrast with IBM MQ)

**Status:** Accepted

**Context**

IBM MQ has a DestinationRule because its web console port (9443) is HTTPS and Istio must re-originate TLS after terminating the client's connection on port 8443 (see ADR-011). Tibco EMS has no web console accessible through the Istio gateway. The only external route for Tibco is the TLS PASSTHROUGH on port 443 to pod port 7243 — Istio does not terminate TLS for this route, so there is nothing to re-originate.

**Decision**

No DestinationRule is created for Tibco instances. The Istio VirtualService for Tibco contains only a TLS PASSTHROUGH route:

```yaml
spec:
  tls:
  - match:
    - sniHosts: ["servera.tibco.margin.service-virtualization.local"]
    route:
    - destination:
        host: margin-tibco-service-virtualization-tibco-serverA.margin.svc.cluster.local
        port:
          number: 7243
```

**Consequences**

- The Tibco Istio configuration is simpler than IBM MQ's — one VirtualService per instance, no DestinationRule
- If a Tibco management console were ever exposed through the gateway (not currently the case), a DestinationRule would need to be added following the IBM MQ console pattern

---

### ADR-015: Hostname Verification Disabled for Backend-to-Tibco Internal Connections

**Status:** Accepted

**Context**

The backend connects to Tibco EMS using the Tibco JMS client (`TibjmsConnectionFactory`). When `ssl://` is used in the URL, the Tibco client performs full TLS including hostname verification — it checks that the server's certificate CN or SAN matches the hostname in the URL. The backend uses the internal Kubernetes Service FQDN:

```
ssl://margin-tibco-service-virtualization-tibco-serverA.margin.svc.cluster.local:7222
```

The wildcard certificate covers `*.service-virtualization.local`. The Kubernetes FQDN (`*.svc.cluster.local`) is a different domain — it does not match. Hostname verification would always fail.

Disabling hostname verification is acceptable here because:
1. The connection is intra-cluster — it does not traverse a public network
2. The TLS layer still provides encryption and certificate authentication; only the hostname check is skipped
3. Hostname verification is designed to protect against man-in-the-middle attacks on untrusted networks; Kubernetes pod-to-pod traffic does not face this threat

**Decision**

`TibjmsConnectionFactory.setSSLCheckHostname(false)` is called when `ssl.verifyHostname: false` is set in the Tibco registry configuration:

```yaml
TIBCO_REGISTRY_SERVERA_SSL_VERIFY_HOSTNAME: "false"
```

The Tibco client still validates that the server presents a certificate signed by the trusted CA (the wildcard cert's issuer in the JKS truststore) — only the hostname check is skipped.

**Consequences**

- The backend still uses encrypted connections to Tibco even with hostname verification disabled — confidentiality is preserved
- This setting must not be used for connections that traverse untrusted networks (e.g., external Tibco clusters) — in those cases the correct hostname must be used and verification must be enabled
- The setting is clearly documented in the values file with a comment explaining why it is disabled

---

## Apache Kafka

---

### ADR-016: KRaft Mode — No Zookeeper

**Status:** Accepted

**Context**

Kafka historically required a ZooKeeper ensemble for cluster metadata management and controller election. Deploying ZooKeeper alongside Kafka added operational complexity: additional pods, separate configuration, and a separate startup ordering dependency. For a service virtualization platform where Kafka instances are lightweight (single-node, single-broker), ZooKeeper adds significant overhead relative to its value.

KRaft (Kafka Raft metadata) was introduced in Kafka 2.8 as an early-access feature and became production-ready in Kafka 3.3. It replaces ZooKeeper with an in-process Raft consensus mechanism using a dedicated controller port (9093).

**Decision**

All Kafka instances use KRaft mode. Each pod runs as both broker and controller. ZooKeeper is not deployed. The Confluent `cp-kafka` 7.4.0 image supports KRaft natively.

Each instance requires a unique `clusterID` (22-character base64URL-encoded UUID) that is set at volume initialisation and cannot be changed without wiping data:

```yaml
kafka:
  instances:
    events:
      clusterID: "MkU3OEVBNTcwNTJENDM2Qk"
```

**Consequences**

- Each Kafka instance is a single pod with no external coordination dependencies
- The `clusterID` is immutable for the lifetime of the Kafka data volume — changing it requires deleting the pod and any persistent state
- Kafka 4.0 has removed ZooKeeper support entirely — this decision future-proofs the platform
- KRaft requires the controller port (9093) to be reachable from within the pod itself; the Service exposes it, but it is not routed externally

---

### ADR-017: Confluent `cp-kafka` Image Over Plain Apache Kafka

**Status:** Accepted

**Context**

The Apache Kafka project does not publish an official Docker image. Multiple community images exist, each with different configuration conventions and tooling. The Confluent Platform `cp-kafka` image is the most widely documented, has a published and stable environment variable API, and includes all required tooling (`kafka-storage`, `openssl`, `keytool`) in a single image — which is critical for the init container pattern used to convert PEM certificates to JKS format (ADR-020 describes this pattern for the backend; the same applies to Kafka).

**Decision**

All Kafka instances use `confluentinc/cp-kafka:7.4.0`. The image is also used for the certificate converter init container (no additional image pull needed):

```yaml
# Init container uses same image — already pulled, no extra pull time
- name: cert-converter
  image: confluentinc/cp-kafka:7.4.0
  command: ["/bin/sh", "-c", "keytool -importcert ... && keytool -importkeystore ..."]
```

The Confluent environment variable conventions (`KAFKA_ADVERTISED_LISTENERS`, `KAFKA_SSL_KEYSTORE_FILENAME`, etc.) are well-documented and stable across versions.

**Consequences**

- Confluent `cp-kafka` is Confluent's licensed product wrapper; the `7.4.0` image is freely usable under the Confluent Community License for non-production use — this must be verified before using in any commercial deployment
- The init container reuses the already-pulled image, meaning no additional pull latency or registry bandwidth for certificate conversion
- Upgrading Kafka requires updating the image tag and re-validating that KRaft and SSL environment variables have not changed between versions

---

### ADR-018: Backend Connects to Kafka via PLAINTEXT Port 9092, Not SSL

**Status:** Accepted

**Context**

Kafka's advertised listener mechanism is central to how the protocol works: when a client connects to the bootstrap server, the broker returns metadata containing the advertised address for each partition's leader. The client then disconnects from the bootstrap server and reconnects directly to the advertised address.

The SSL listener is configured with:
```
KAFKA_ADVERTISED_LISTENERS=SSL://events.kafka.margin.service-virtualization.local:443
```

This external hostname is correct for clients connecting through Istio from outside the cluster. However, for the backend pod connecting from inside the cluster:

1. The initial bootstrap connects to the ClusterIP service (resolvable inside the cluster)
2. The broker returns the advertised SSL address: `events.kafka.margin.service-virtualization.local:443`
3. The client disconnects and tries to reconnect to `events.kafka.margin.service-virtualization.local`
4. CoreDNS inside the cluster cannot resolve this name — it is defined in `/etc/hosts` on the Vagrant VM host, not as a DNS record visible to pods
5. Every reconnect after bootstrap fails with `UnknownHostException`

The PLAINTEXT listener is configured with:
```
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://<release>-kafka-events.margin.svc.cluster.local:9092
```

This is the ClusterIP FQDN, which CoreDNS resolves correctly from any pod.

**Decision**

The backend connects to Kafka using the PLAINTEXT listener on port 9092. SSL is not used for in-cluster backend-to-Kafka connections:

```yaml
KAFKA_REGISTRY_EVENTS_BOOTSTRAP_SERVERS: "margin-kafka-service-virtualization-kafka-events.margin.svc.cluster.local:9092"
KAFKA_REGISTRY_EVENTS_SSL_ENABLED: "false"
```

External clients (test tools, CI jobs) continue to use the SSL listener via Istio at port 443.

**Consequences**

- Traffic between the backend pod and the Kafka pod is unencrypted at the application layer. Network-level security (Kubernetes NetworkPolicy, Istio mTLS for sidecar-to-sidecar traffic) is the control in place.
- This is an intentional architectural boundary: TLS is enforced at the cluster boundary (via Istio), not for intra-cluster pod-to-pod traffic
- The decision is documented explicitly in `backend-values.yaml` with a multi-line comment explaining the advertised listener problem — future operators must not change `SSL_ENABLED` to `true` without first solving the DNS resolution issue
- On AKS, if Azure Private DNS resolves the external Kafka hostname inside the cluster, this restriction can be revisited

---

## Backend

---

### ADR-019: Registry-Based Multi-Server Configuration via Environment Variables

**Status:** Accepted

**Context**

The backend needs to connect to multiple instances of each messaging protocol (two IBM MQ instances, two Tibco instances, two Kafka clusters). A traditional Spring Boot approach uses a single `@ConfigurationProperties` binding for each protocol, supporting only one server address. Extending this to N servers requires either a list binding (which cannot be merged across deployments — see ADR-006) or a map binding that requires structural changes to every protocol's connection factory.

**Decision**

Each protocol implements a registry pattern. Environment variables follow the convention:

```
<PROTOCOL>_REGISTRY_<NAME>_<PROPERTY>
```

For example:
```
IBMMQ_REGISTRY_ICG_HOST=...
IBMMQ_REGISTRY_ICG_PORT=...
IBMMQ_REGISTRY_ICG_CHANNEL=...
IBMMQ_REGISTRY_RTO_HOST=...
```

The registry class (`IbmMqServerRegistry`, `TibcoServerRegistry`, `KafkaServerRegistry`) scans all environment variables at startup, groups them by the `<NAME>` segment, and creates a connection factory per discovered name. The `<NAME>` values (ICG, RTO, SERVERA, EVENTS, etc.) correspond directly to the instance names defined in the Helm values.

**Consequences**

- Adding a new server instance requires only adding new environment variables in the values file — no code changes
- The registry is self-discovering: it does not need to know the instance names in advance
- Naming is case-normalised: `IBMMQ_REGISTRY_ICG_HOST` and `ibmmq.registry.icg.host` resolve to the same instance name `icg`
- The pattern is consistent across IBM MQ, Tibco, and Kafka — teams apply the same mental model regardless of protocol

---

### ADR-020: JKS Truststore Built at Pod Startup via Init Container

**Status:** Accepted

**Context**

Java's TLS stack requires a JKS (Java KeyStore) truststore to validate server certificates. The wildcard certificate is stored as a Kubernetes Secret in PEM format (`tls.crt`). Converting PEM to JKS at image build time is not viable — the certificate changes when it is rotated, and rebuilding the Docker image for a certificate rotation is operationally unacceptable.

The JKS could be generated by a separate script run by an operator and stored as a separate Secret, but this adds a manual step to every certificate rotation and introduces a Secret whose content is derived from another Secret (duplication and synchronisation risk).

**Decision**

An init container (`truststore-creator`) runs at pod startup before the main application container. It uses the `eclipse-temurin:21-jre-alpine` image (same JRE as the main container, already pulled) and runs `keytool` to import `tls.crt` from the team TLS Secret into a JKS file written to an `emptyDir` volume:

```yaml
initContainers:
  - name: truststore-creator
    image: eclipse-temurin:21-jre-alpine
    command:
      - /bin/sh
      - -c
      - |
        keytool -import -file /tmp/tls/tls.crt \
          -keystore /app/certs/truststore.jks \
          -storetype JKS -storepass changeit -noprompt -alias sv-wildcard-ca
    volumeMounts:
      - name: margin-tls-secret
        mountPath: /tmp/tls
        readOnly: true
      - name: app-truststore
        mountPath: /app/certs
```

The main container mounts the same `emptyDir` read-only and is started with:
```
-Djavax.net.ssl.trustStore=/app/certs/truststore.jks
-Djavax.net.ssl.trustStorePassword=changeit
```

**Consequences**

- Certificate rotation requires only: update the Kubernetes Secret, then do a rolling restart of the pod — no image rebuild, no manual `keytool` command
- The `emptyDir` volume is ephemeral — the JKS is rebuilt on every pod start, ensuring it always reflects the current Secret content
- The init container reuses the already-pulled JRE image — no additional image pull
- All Java TLS connections from the backend (IBM MQ, Tibco) use this single truststore, meaning all services in a namespace must use certificates signed by the same CA (which is satisfied by the shared wildcard cert strategy)

---

## UI

---

### ADR-021: Runtime Path-Prefix Detection — No Build-Time Environment Variables

**Status:** Accepted

**Context**

The UI is served at a team-specific path prefix through Istio (e.g., `/margin/` for the margin team, `/collateral/` for the collateral team). Two things depend on knowing this prefix at runtime:

1. **API base URL**: `configLoader.ts` constructs the API URL as `<prefix>/api`. If the prefix is `/margin/`, API calls must go to `/margin/api/stubs`, which Istio routes to the backend. If calls go to `/api/stubs` instead, Istio has no matching route.

2. **React Router base path**: `BrowserRouter` must know the prefix so that all internal navigation links are generated with the prefix (e.g., `<Link to="/stubs">` must render as `/margin/stubs`, not `/stubs`). Without this, clicking a link navigates the browser to `/stubs`, which Istio cannot route.

The prefix could be injected at build time as an environment variable baked into the webpack bundle, but this would require building a different Docker image per team — defeating the "one image, multiple teams" goal.

Alternatively, the prefix could be injected by nginx via `envsubst` into the HTML file at container start, but this would require modifying `index.html` to reference a JavaScript variable, adding complexity.

**Decision**

`configLoader.ts` detects the prefix at runtime from `window.location.pathname`:

```typescript
export const getBasePath = (): string => {
  const currentPath = window.location.pathname;
  if (currentPath.startsWith('/margin'))     return '/margin';
  if (currentPath.startsWith('/collateral')) return '/collateral';
  if (currentPath.startsWith('/sv'))         return '/sv';
  return '/';
};
```

`index.tsx` passes the same value as the `BrowserRouter` basename:

```tsx
<BrowserRouter basename={getBasePath()}>
  <App />
</BrowserRouter>
```

The same Docker image is deployed for both margin and collateral teams. The prefix is detected from the URL the browser presents — which Istio determines by which VirtualService serves the request.

**Consequences**

- A single Docker image works for all teams with no rebuild per team
- Adding a new team requires adding a new path detection clause in `configLoader.ts` — a one-line code change and a new image build (unavoidable, but minimal)
- The detection logic must be updated before deploying the platform for a new team; there is no runtime fallback if the prefix is unknown
- When the UI is accessed at root (`/`) — for example via direct port-forward during local development — `getBasePath()` returns `/`, `BrowserRouter` uses `/` as its base, and `getApiBaseUrl()` returns `/api`. nginx's `proxy_pass ${BACKEND_URL}` handles `/api/` requests in that case, providing a working local development experience without Istio

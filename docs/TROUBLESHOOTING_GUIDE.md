# Troubleshooting Guide

This guide covers issues encountered across all layers of the platform — infrastructure, Kubernetes, Helm, Istio routing, and each messaging protocol.

---

## Table of Contents

- [Vagrant and VM](#vagrant-and-vm)
- [Kubernetes and K3s](#kubernetes-and-k3s)
- [Terraform](#terraform)
- [Helm](#helm)
- [Istio and Routing](#istio-and-routing)
- [TLS Certificates](#tls-certificates)
- [IBM MQ](#ibm-mq)
- [Tibco EMS](#tibco-ems)
- [Apache Kafka](#apache-kafka)
- [Backend (Spring Boot)](#backend-spring-boot)
- [MongoDB](#mongodb)
- [UI](#ui)

---

## Vagrant and VM

### `vagrant up` fails with VT-x/AMD-v not enabled

VirtualBox requires hardware virtualisation to be enabled in BIOS/UEFI.

- Reboot your machine, enter BIOS, and enable "Intel VT-x" or "AMD-V / SVM"
- On Windows 11, also check that Hyper-V is disabled if VirtualBox is reporting conflicts:
  ```powershell
  bcdedit /set hypervisorlaunchtype off
  # Reboot required
  ```

### VM starts but `vagrant ssh` shows a bridge network warning

```
No IP address found for eth1
```

The VM could not get a DHCP lease on the bridged adapter.

1. Run `vagrant ssh -c "ip a"` and check which interfaces are up
2. If `eth1` has no IP, try a different host network adapter in the Vagrantfile:
   ```ruby
   config.vm.network "public_network", bridge: "Intel(R) Wi-Fi 6 AX201"
   ```
3. On some machines it is simpler to use host-only networking and update `kubeconfig-bridge.yaml` to `127.0.0.1:6443`

### `kubectl get nodes` shows `NotReady` after `vagrant up`

K3s may still be initialising. Wait 30 seconds and retry. If it remains `NotReady`:

```powershell
vagrant ssh -c "sudo systemctl status k3s"
vagrant ssh -c "sudo journalctl -u k3s -n 50"
```

Look for disk pressure or memory pressure events — the default 8 GB RAM is tight if other VMs are running.

### VM is very slow

VirtualBox performance degrades significantly when host memory is under pressure. Close other applications and VMs. Do not set the VM RAM below 6 GB.

---

## Kubernetes and K3s

### Pod stuck in `ContainerCreating`

```powershell
kubectl describe pod -n <namespace> <pod-name>
```

Common causes:

| Event message | Cause | Fix |
|---|---|---|
| `ImagePullBackOff` | Image not found or registry unreachable | Verify `image.repository` and `image.tag` in values file. Check `acr-secret` image pull secret exists. |
| `Failed to mount secret` | Named secret does not exist in the namespace | Create the secret (e.g., `margin-tls-secret`) — see Phase 3 of the deployment guide |
| `failed to create containerd task` | AppArmor / seccomp conflict | For IBM MQ: set `global.securityContext: {}` in `sv-global.yaml`. IBM MQ's internal user (UID 1001) conflicts with pod-level securityContext. |

### Pod in `CrashLoopBackOff`

```powershell
kubectl logs -n <namespace> <pod-name> --previous
```

For init container failures:
```powershell
kubectl logs -n <namespace> <pod-name> -c <init-container-name>
```

### `kubectl` cannot connect to cluster

```powershell
kubectl get nodes
# error: The connection to the server ... was refused
```

- Verify `$env:KUBECONFIG` points to `kubeconfig-bridge.yaml`
- Check the VM is running: `vagrant status`
- Verify the VM's bridge IP matches the server URL in the kubeconfig:
  ```powershell
  vagrant ssh -c "hostname -I"
  Get-Content deployments\local\cluster-admin\kubeconfig-bridge.yaml | Select-String "server:"
  ```

---

## Terraform

### `terraform apply` fails with kubeconfig error

```
Error: Post "https://127.0.0.1:6443/api/v1/namespaces": dial tcp 127.0.0.1:6443: connect: connection refused
```

When running Terraform from Windows (not inside the VM), update `kubeconfig_path` in `terraform.tfvars` to use `kubeconfig-bridge.yaml` instead of `kubeconfig-local.yaml`:

```hcl
kubeconfig_path    = "../../kubeconfig-bridge.yaml"
kubeconfig_context = "default"
```

### `shared-gateway` apply fails with "no kind Gateway is registered"

The Gateway CRD does not exist yet — `base/` must be applied first:

```powershell
cd deployments\local\cluster-admin\terraform\istio\base
terraform apply
# Then:
cd ..\shared-gateway
terraform apply
```

### `terraform destroy` for `base/` hangs

Istio's webhooks can block namespace deletion. If destroy hangs, force-delete the stuck namespace:

```powershell
kubectl get namespace istio-system -o json | `
  ConvertFrom-Json | `
  ForEach-Object { $_.spec.finalizers = @(); $_ | ConvertTo-Json -Depth 10 } | `
  kubectl replace --raw /api/v1/namespaces/istio-system/finalize -f -
```

---

## Helm

### `Error: INSTALLATION FAILED: ... is required`

All values marked `required` in the chart must be present. The error message names the missing key. Either:
- Add the key to your values file, or
- Verify you are passing both `sv-global.yaml` and the service-specific values file with `-f`

### `Error: rendered manifests contain a resource that already exists`

A previous install left resources behind (e.g., a failed upgrade). Use `--force` or delete the conflicting resource manually:

```powershell
helm uninstall <release> -n <namespace>
helm install <release> ...
```

### Helm upgrade does not pick up config changes

After changing a ConfigMap-mounted config file, pods are not automatically restarted. Add a checksum annotation to force a rolling restart:

```powershell
kubectl rollout restart deployment/<name> -n <namespace>
```

### `UPGRADE FAILED: VirtualService is invalid: metadata.name: Invalid value ... must consist of lower case alphanumeric characters`

**Full error example:**
```
Error: UPGRADE FAILED: failed to create resource: VirtualService.networking.istio.io
"margin-tibco-serverA-vs" is invalid: metadata.name: Invalid value:
"margin-tibco-serverA-vs": a lowercase RFC 1123 subdomain must consist of lower case
alphanumeric characters, '-' or '.'
```

**Cause:** Kubernetes resource names (VirtualService, Service, Deployment, ConfigMap) must be fully lowercase (RFC 1123). If a Helm values file uses a map key with mixed case (e.g., `instances.serverA`), and the template interpolates it directly into `metadata.name` without a `| lower` filter, the resulting name contains uppercase letters and Kubernetes rejects it.

**Fix:** The Helm templates already apply `| lower` to all instance name references in `metadata.name`, label values, and destination host fields. If you see this error after adding a new instance in a values file, verify the instance key does not require uppercasing. Prefer lowercase map keys in values files:

```yaml
# Preferred (safe)
instances:
  servera:
    ...

# Also works (templates apply | lower)
instances:
  serverA:
    ...
```

If you have deployed the release previously with the incorrect capitalised name, you may need to delete the stale resource manually before re-running the upgrade:

```powershell
kubectl delete virtualservice margin-tibco-serverA-vs -n margin
helm upgrade margin $MARGIN_UMBRELLA -f ... --namespace margin
```

---

## Istio and Routing

### VirtualService not matching — traffic returns 404 or connection refused

```powershell
# Check the VirtualService was created in the correct namespace
kubectl get virtualservice -n <namespace> -o yaml

# Check the Gateway name and namespace match exactly
kubectl get gateway -n istio-ingress

# Check Istio's view of the routing table
istioctl proxy-config routes deploy/istio-ingressgateway -n istio-ingress
```

Common mistakes:
- VirtualService references `gateways: ["istio-ingress/shared-gateway"]` but the Gateway was created in a different namespace
- `sniHosts` value does not match the hostname the client presents — the SNI hostname must be byte-for-byte identical to what appears in the TLS `ClientHello`

### SNI routing sends traffic to the wrong pod

Each TLS VirtualService must have a unique `sniHosts` entry. If two VirtualServices share the same SNI host, only one will match (behaviour is undefined).

Check for duplicates:
```powershell
kubectl get virtualservice -A -o jsonpath='{range .items[*]}{.metadata.name}{": "}{.spec.tls[*].match[*].sniHosts}{"\n"}{end}'
```

### HTTP routing fails after Istio upgrade

Check if the `Gateway` spec uses the correct `protocol` value. `protocol: HTTPS` is for HTTP-over-TLS (Istio terminates TLS and speaks HTTP upstream). `protocol: TLS` with `mode: PASSTHROUGH` passes raw bytes — do not mix these for HTTP services.

### `istioctl proxy-config` shows no routes for a service

The Istio sidecar in the ingress gateway may not have received the updated config yet. Wait 10–15 seconds and retry. If the issue persists:

```powershell
kubectl rollout restart deployment/istio-ingressgateway -n istio-ingress
```

---

## TLS Certificates

### `openssl s_client` returns "handshake failure"

Possible causes:

1. **`gateway-tls-cert` secret is missing** in `istio-ingress`:
   ```powershell
   kubectl get secret gateway-tls-cert -n istio-ingress
   ```

2. **Wrong NodePort** — connecting to the HTTP NodePort (port 80 equivalent) instead of the TLS NodePort (port 443 equivalent):
   ```powershell
   kubectl get svc istio-ingressgateway -n istio-ingress
   # Use the NodePort mapped to 443, not 80
   ```

3. **DNS resolves to loopback (`127.0.0.1`) instead of the bridge IP** — the Vagrant port-forwards only cover specific ports. If your DNS entry points to `127.0.0.1`, the NodePort for TLS may not be forwarded. Use the bridge IP.

### Self-signed certificate warning in Java client

This is expected for local development. The Java client must trust the self-signed cert via a JKS truststore:

```powershell
keytool -import -file wildcard.crt -alias gateway-ca `
  -keystore truststore.jks -storepass changeit -noprompt
```

Then pass to the JVM:
```
-Djavax.net.ssl.trustStore=truststore.jks
-Djavax.net.ssl.trustStorePassword=changeit
```

### Init container `truststore-creator` fails

```powershell
kubectl logs -n <namespace> <backend-pod> -c truststore-creator
```

Common causes:
- `margin-tls-secret` does not exist in the namespace — create it (Phase 3 of deployment guide)
- The secret key name is wrong — the template expects `tls.crt` (standard `kubernetes.io/tls` secret key name)

---

## IBM MQ

### `MQRC_NOT_AUTHORIZED` (reason code 2035)

This error means the queue manager accepted the TLS handshake but rejected the client's identity.

**Most common cause — CHLAUTH blocking the connection:**

IBM MQ's default `CHLAUTH` rules block connections where the client's OS username does not match a permitted user. The fix is to set `MCAUSER` on the channel to a known MQ user and disable OS authentication:

```mqsc
ALTER CHANNEL(DEV.APP.SVRCONN) CHLTYPE(SVRCONN) MCAUSER('app')
SET CHLAUTH(DEV.APP.SVRCONN) TYPE(BLOCKUSER) USERLIST('nobody')
ALTER QMGR CONNAUTH(' ')
REFRESH SECURITY TYPE(CONNAUTH)
```

This is already included in the auto-generated MQSC when `config.default.enabled: true`.

**Secondary cause — `SSLPeerName` mismatch:**

If `SSLPeerName` is set on the MQ channel, the certificate's CN or SAN must match. For the wildcard cert (`CN=*.service-virtualization.local`), either clear `SSLPeerName` on the channel or ensure the Java client does not set it.

### IBM MQ queue manager not starting

```powershell
kubectl logs -n margin <mq-pod-name>
```

Look for `AMQ` error codes:
- `AMQ7017E: Log not available` — volume permissions issue; check `securityContext` is empty (`{}`)
- `AMQ5806I: ... license not accepted` — set `license: "accept"` in values
- `AMQ9213E: A remote host refused a connection` — the pod cannot reach its own queue manager internally; this is usually a startup ordering issue, wait for the pod to fully initialise

### IBM MQ SSL cipher suite mismatch

The cipher suite must match between the server-side MQSC config and the Java client:

| Java client property | Expected value |
|---|---|
| `SSLCipherSuite` on `MQQueueConnectionFactory` | `TLS_RSA_WITH_AES_256_CBC_SHA256` |
| JVM flag | `-Dcom.ibm.mq.cfg.preferTLS=true` |
| MQSC on channel | `SSLCIPH(TLS_RSA_WITH_AES_256_CBC_SHA256)` |

If the cipher suite is `NONE` or empty on either side, TLS will not be established.

### IBM MQ pod `READY 0/1` after `Running`

The readiness probe is failing. IBM MQ's queue manager takes ~60–90 seconds to fully start. The readiness probe checks `dspmq` and will return not-ready until the queue manager reports `STATUS(Running)`.

```powershell
kubectl describe pod -n margin <mq-pod-name>
# Look at "Readiness" probe results
```

Increase `readinessProbe.initialDelaySeconds` if the probe fires before the queue manager is ready.

---

## Tibco EMS

### Tibco connection fails with "SSL vendor not set"

The backend must call `factory.setSSLVendor("j2se")` before setting any other SSL properties. This is handled in `TibcoServerRegistry.configureSsl()` — verify `ssl.enabled: true` is set in the Tibco registry env vars:

```yaml
TIBCO_REGISTRY_SERVERA_SSL_ENABLED: "true"
```

### Tibco SSL connection fails with "hostname verification failed"

Tibco EMS by default verifies that the server's certificate CN/SAN matches the hostname used in the connection URL. Inside Kubernetes, the connection URL uses the ClusterIP service FQDN (e.g., `margin-app-...-tibco-servera.margin.svc.cluster.local`) which does not match the wildcard cert (`*.service-virtualization.local`).

Disable hostname verification for internal connections:

```yaml
TIBCO_REGISTRY_SERVERA_SSL_VERIFY_HOSTNAME: "false"
```

This is safe for intra-cluster communication where the network is already secured by Kubernetes network policies and Istio mTLS.

### Tibco SSL listener ignored: "Failed to initialize TLS: unable to obtain password"

**Symptom:** The EMS pod starts and accepts TCP connections on port 7222, but SSL on port 7243 is skipped with:
```
ERROR: Failed to initialize TLS: unable to obtain password
WARNING: Ignoring TLS listen port ssl://0.0.0.0:7243
```

**Cause:** TIBCO EMS requires a non-empty `ssl_password` value in `tibemsd.conf`. When the value is blank or absent it falls back to prompting stdin interactively. Containers have no stdin, so the prompt immediately fails, and EMS skips the SSL listener rather than crashing.

This is **not** a missing or corrupted certificate. The cert and key files are present and readable; EMS simply will not proceed without a password in config.

**A related error** appears if `ssl_password` is non-empty but `ssl_server_identity` still points to a PEM file:
```
ERROR: Unable to load identity certificate from file '/etc/tibco/certs/tls.crt': invalid data or password
```
This confirms TIBCO EMS's behaviour: **non-empty `ssl_password` → expects PKCS12**. The init container must use `openssl pkcs12 -export` to produce `server.p12`, and `ssl_server_identity` must point to that `.p12` file. PEM files are only used when `ssl_password` is absent (which fails in containers as described above).

**Fix:** Set `certificates.password` in the instance values file:
```yaml
certificates:
  enabled: true
  password: "changeit"   # any non-empty string for unencrypted keys
```

For an **unencrypted** private key (`BEGIN RSA PRIVATE KEY` or `BEGIN PRIVATE KEY`) any non-empty string works — EMS passes the value to the key loader, which ignores it for unencrypted keys.

For an **encrypted** private key (`BEGIN ENCRYPTED PRIVATE KEY`) set this to the actual passphrase used when the key was generated.

The `certificates.password` field is **mandatory** — `helm template` will fail with a validation error if it is missing when `certificates.enabled: true`.

---

### Tibco pod shows "TIBCO EMS Server is already running"

A previous instance left the EMS data files in the persistent volume. Either:
- Delete and recreate the pod (triggers a fresh startup if no PVC)
- Or delete the PVC if one exists, then delete the pod

Tibco instances in this platform do not use PVCs by default — the volume is ephemeral. If you see this error, it means a second pod was started before the first fully stopped during a rolling upgrade. Use `kubectl rollout status` to confirm the old pod has terminated.

---

## Apache Kafka

### Kafka consumer fails to reconnect after initial connect

**Symptom:** The backend connects successfully on startup but then loses the connection and cannot reconnect. Logs show `LEADER_NOT_AVAILABLE` or `UNKNOWN_HOST`.

**Root cause:** Kafka's SSL listener advertises its `externalHostname` (e.g., `events.kafka.margin.service-virtualization.local`) in broker metadata. When an internal pod connects via SSL on port 9094, Kafka tells it to reconnect to the external hostname — which is not resolvable via CoreDNS inside the cluster.

**Fix:** The backend connects to Kafka using the **PLAINTEXT listener on port 9092**, not SSL. The PLAINTEXT listener advertises the internal ClusterIP FQDN which is resolvable. Set `SSL_ENABLED: false` in the backend's Kafka registry config:

```yaml
KAFKA_REGISTRY_EVENTS_SSL_ENABLED: "false"
KAFKA_REGISTRY_EVENTS_BOOTSTRAP_SERVERS: "margin-app-service-virtualization-kafka-events.margin.svc.cluster.local:9092"
```

External clients (test tools, CI jobs running outside the cluster) connect via port 9094 through Istio TLS PASSTHROUGH and use the external hostname — this path does work with SSL.

### Kafka pod fails to start — "KRaft metadata log" error

Each Kafka instance requires a unique `clusterID`. If two instances share the same ID, the controller election fails. Generate a unique ID per instance:

```bash
# Run inside any Kafka container to generate a valid KRaft cluster ID
kafka-storage random-uuid
```

Set the result as `kafka.instances.<name>.clusterID` in the values file.

### Kafka topic not created automatically

By default the Kafka instances have `auto.create.topics.enable=true`. If a topic is not appearing:

```powershell
# Exec into the Kafka pod and list topics
kubectl exec -n margin <kafka-pod> -- kafka-topics --bootstrap-server localhost:9092 --list

# Create a topic manually if needed
kubectl exec -n margin <kafka-pod> -- kafka-topics `
  --bootstrap-server localhost:9092 `
  --create --topic my-topic --partitions 1 --replication-factor 1
```

---

## Backend (Spring Boot)

### Application fails to start — "Could not resolve placeholder"

Spring Boot throws this on startup if an environment variable referenced in `application.yml` as `${VAR_NAME}` is not set and has no default.

All optional messaging service variables have safe defaults (empty string or `0`) in `application.yml`. If you see this error, a required variable is missing. Check your `backend-values.yaml` — every `IBMMQ_REGISTRY_*`, `TIBCO_REGISTRY_*`, and `KAFKA_REGISTRY_*` variable that is referenced in the registry initialiser must be present.

### Backend cannot connect to IBM MQ — SSL handshake exception

1. Confirm the truststore was built correctly by the init container:
   ```powershell
   kubectl exec -n margin <backend-pod> -- ls -la /app/certs/
   # Should show truststore.jks
   ```

2. Confirm JAVA_OPTS are set:
   ```powershell
   kubectl exec -n margin <backend-pod> -- printenv JAVA_OPTS
   # Should include -Djavax.net.ssl.trustStore=/app/certs/truststore.jks
   ```

3. Confirm `IBMMQ_REGISTRY_ICG_SSL_ENABLED` is `"true"` and `IBMMQ_REGISTRY_ICG_HOST` points to the internal service FQDN

### Backend cannot reach Tibco — connection refused on port 7222

The backend connects to Tibco on the plain TCP listener (port 7222). A common mistake is using `ssl://` on port 7222, which sends an SSL ClientHello to a plain-TCP port and gets immediately rejected.

Correct internal configuration:
```yaml
TIBCO_REGISTRY_SERVERA_URL: "tcp://margin-app-...-tibco-servera.margin.svc.cluster.local:7222"
TIBCO_REGISTRY_SERVERA_SSL_ENABLED: "false"
```

Port 7243 (SSL) is only for external clients routing through Istio TLS PASSTHROUGH. Do not use port 7243 for the backend.

---

## MongoDB

### Backend pod stuck in `Init:0/1` — truststore-creator init container fails

The `truststore-creator` init container reads from the team TLS secret. If the secret does not exist, the init container errors immediately:

```powershell
kubectl describe pod -n margin <backend-pod>
# Look for: "secret margin-tls-secret not found"
```

Create the secret first:
```powershell
kubectl create secret generic margin-tls-secret \
  --from-file=tls.crt=deployments/local/cluster-admin/wildcard.crt \
  --from-file=tls.key=deployments/local/cluster-admin/wildcard.key \
  -n margin
```

### Backend pod `Running` but readiness probe fails — `MongoTimeoutException`

The backend attempts a MongoDB connection on startup. If MongoDB is not yet deployed or not yet healthy, Spring's `MongoAutoConfiguration` throws a `MongoTimeoutException` and the readiness probe at `/actuator/health` returns 503.

1. Deploy MongoDB before the backend by including `mongodb.yaml` in the umbrella upgrade:
   ```powershell
   $U = "deployments/local/teams/teams/margin/umbrella"
   helm upgrade --install margin $U `
     -f $U/values/sv-global.yaml `
     -f $U/values/mongodb.yaml `
     --namespace margin
   ```

2. Wait for MongoDB to be ready before deploying the core product:
   ```powershell
   kubectl wait --for=condition=available deployment/margin-mongodb -n margin --timeout=120s
   ```

3. If the backend was deployed before MongoDB, force a pod restart after MongoDB is healthy:
   ```powershell
   kubectl rollout restart deployment/margin-app-service-virtualization-backend -n margin
   ```

### `MONGODB_URI` points to wrong host — backend connects to a non-existent service

The `MONGODB_URI` environment variable must match the Kubernetes Service name created by the Helm chart. The service name is controlled by `mongodb.nameOverride` in the values file.

Check what service name was created:
```powershell
kubectl get svc -n margin | grep mongodb
# e.g. margin-mongodb   ClusterIP   ...   27017/TCP
```

The `MONGODB_URI` must match:
```yaml
MONGODB_URI: "mongodb://margin-mongodb.margin.svc.cluster.local:27017"
```

If the service name does not match, either update `mongodb.nameOverride` in the values file and redeploy MongoDB, or update `MONGODB_URI` in `backend-values.yaml` and redeploy the backend.

### Bring-your-own MongoDB / Sybase — disabling the built-in MongoDB

To use an external MongoDB or Cosmos DB, set `mongodb.enabled: false` (or omit the MongoDB values file entirely) and point the backend at your instance:

```yaml
# backend-values.yaml
env:
  MONGODB_URI: "mongodb://your-external-host:27017/service_virtualization"
```

For Sybase/MSSQL: remove `SPRING_AUTOCONFIGURE_EXCLUDE` for `DataSourceAutoConfiguration` and configure the appropriate JDBC datasource properties instead.

---

## UI

### UI loads but API calls return 404

The browser is calling `/api/...` but Istio has no route for that path. This happens when:
- The UI is accessed via a path prefix (e.g., `/margin/`) through Istio
- `configLoader.ts` correctly returns `/margin/api` for that prefix
- But the backend VirtualService routes `/margin/api/` (not `/api/`)

Verify the browser is resolving to the correct path. Open browser DevTools → Network tab and check the Request URL of any failing API call. It should be `/margin/api/...`, not `/api/...`.

If calls are going to `/api/...`, the UI is being accessed at root `/` rather than the prefixed path — which means the browser URL path does not start with `/margin/` and `configLoader.ts` falls back to `/api`.

### React Router navigation breaks after clicking a link

This occurs when the UI is accessed via an Istio path prefix (`/margin/`) but `BrowserRouter` is not initialised with the correct `basename`. After clicking a link, React Router generates an href like `/dashboard` (relative to root) and the browser navigates to `http://host/dashboard` — which Istio has no route for.

`configLoader.ts` exports `getBasePath()` and `index.tsx` passes it to `BrowserRouter` as `basename`. Verify both files reflect the current path prefix detection logic.

### nginx returns 502 Bad Gateway for API calls

When running the UI container locally without Istio (e.g., `docker run`), nginx proxy_pass handles `/api/` requests. A 502 means `BACKEND_URL` is unreachable from the container.

```bash
# Check the BACKEND_URL env var was substituted correctly at startup
docker exec <container-id> cat /etc/nginx/nginx.conf | grep proxy_pass
```

In Kubernetes, `BACKEND_URL` is set in `ui-values.yaml` to the backend ClusterIP FQDN. Verify the backend pod is running and its service exists:

```powershell
kubectl get svc -n margin | grep backend
kubectl get pods -n margin -l app.kubernetes.io/component=backend
```

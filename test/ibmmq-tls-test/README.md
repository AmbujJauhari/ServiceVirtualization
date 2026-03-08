# IBM MQ TLS Integration Tests

Tests that validate IBM MQ TLS connectivity through Istio SNI passthrough routing
for both the `margin` and `collateral` team instances.

## What is Being Tested

```
On-prem test (this JVM)
    │
    │  TLS (port 443)
    │  SNI=ibmmq.margin.service-virt.local  ─────────────────────────►  margin MQ pod
    │  SNI=ibmmq.collateral.service-virt.local  ─────────────────────►  collateral MQ pod
    │                         ▲
    │               Istio ingressgateway
    │               reads SNI, routes to correct pod
    │               without decrypting traffic
    ▼
K3s VM (or AKS cluster)
```

## Prerequisites

### 1. Istio and IBM MQ Deployed

```bash
# Istio
cd deployments/local/cluster-admin/terraform/istio
terraform apply

# IBM MQ (margin)
helm install sv-margin ./helm-charts/service-virtualization \
  -n margin -f values-margin.yaml

# IBM MQ (collateral)
helm install sv-collateral ./helm-charts/service-virtualization \
  -n collateral -f values-collateral.yaml
```

### 2. Hosts File Entry (Local K3s Only)

Add to `C:\Windows\System32\drivers\etc\hosts` (Windows) or `/etc/hosts` (Linux/Mac):

```
10.0.2.15  ibmmq.margin.service-virt.local
10.0.2.15  ibmmq.collateral.service-virt.local
```

Replace `10.0.2.15` with your K3s VM's bridge IP.

### 3. TLS Truststore

The JVM needs to trust the certificate presented by the IBM MQ pods.
Create a JKS truststore from the self-signed cert (or corporate CA cert):

```bash
# Copy the cert from the K8s Secret (or wherever it was generated)
kubectl get secret margin-mq-tls -n margin -o jsonpath='{.data.tls\.crt}' | base64 -d > /tmp/margin-mq.crt

# Create truststore (add both certs or use a shared CA cert)
keytool -import -trustcacerts \
  -file /tmp/margin-mq.crt \
  -alias margin-mq \
  -keystore src/test/resources/certs/truststore.jks \
  -storepass changeit \
  -noprompt

# If collateral uses a different cert, add it too
kubectl get secret collateral-mq-tls -n collateral -o jsonpath='{.data.tls\.crt}' | base64 -d > /tmp/collateral-mq.crt

keytool -import -trustcacerts \
  -file /tmp/collateral-mq.crt \
  -alias collateral-mq \
  -keystore src/test/resources/certs/truststore.jks \
  -storepass changeit \
  -noprompt
```

### 4. Update test.properties

Edit `src/test/resources/test.properties` if your hostnames or ports differ from the defaults.

## Running the Tests

```bash
# From the test/ibmmq-tls-test directory

# Run all tests (integration profile required)
mvn test -Pintegration

# Run a specific test class
mvn test -Pintegration -Dtest=IBMMQTLSConnectionTest

# Run with custom truststore path
mvn test -Pintegration \
  -Dssl.trustStorePath=/path/to/your/truststore.jks \
  -Dssl.trustStorePassword=yourpassword
```

## Test Classes

| Class | What it Tests |
|---|---|
| `IBMMQTLSConnectionTest` | TLS handshake succeeds, both instances reachable on port 443 |
| `IBMMQSNIRoutingTest` | SNI hostname correctly routes to the matching MQ instance |
| `IBMMQMessageFlowTest` | Put/get messages end-to-end, namespace isolation confirmed |

## Troubleshooting

### `PKIX path building failed` (SSL handshake fails)
The JVM does not trust the MQ server's certificate. Make sure the cert is in the truststore:
```bash
keytool -list -keystore src/test/resources/certs/truststore.jks -storepass changeit
```

### `MQJE001: Completion Code '2', Reason '2538'` (host not found / cannot connect)
- Check the hosts file entry points to the correct K3s VM IP
- Verify the Istio ingressgateway NodePort is serving on port 443:
  ```bash
  kubectl get svc -n istio-ingress
  ```
- Verify the shared gateway has a TLS PASSTHROUGH server on port 443:
  ```bash
  kubectl describe gateway shared-gateway -n istio-ingress
  ```

### `MQJE001: Completion Code '2', Reason '2035'` (not authorized)
The MQ channel requires authentication. Set `MQ_ADMIN_PASSWORD` or check the MQSC config.

### `MQJE001: Completion Code '2', Reason '2399'` (SSL cipher mismatch)
The `ssl.cipherSuite` in `test.properties` does not match the `SSLCIPH` on the MQ channel.
Check the channel definition:
```bash
kubectl exec -n margin <mq-pod> -- runmqsc QMGR_MARGIN <<< "DISPLAY CHANNEL(SYSTEM.SSL.SVRCONN) SSLCIPH"
```

### VirtualService not routing
Verify the VirtualService was created:
```bash
kubectl get virtualservice -n margin
kubectl describe virtualservice sv-margin-mq-primary-vs -n margin
```

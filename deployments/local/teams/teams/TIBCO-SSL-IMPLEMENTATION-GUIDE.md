# TIBCO Template SSL Implementation Guide

## Overview

This guide shows exactly how to update `helm-charts/service-virtualization/templates/optional-services/tibco.yaml` to support SSL/TLS.

---

## Key Changes Required

### 1. Add SSL Check at Template Start

```yaml
{{- if .Values.tibco.enabled }}
{{- $sslEnabled := eq (include "service-virtualization.sslEnabled" .) "true" }}
{{- $globalCertConfig := .Values.global.certificates | default dict }}

# Rest of template...
```

### 2. Update ConfigMap for SSL Configuration

**Current** (lines ~30-80):
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}-config
data:
  tibemsd.conf: |
    server = {{ $instance.bootstrap.serverName }}
    listen = tcp://0.0.0.0:{{ $instance.service.tcpPort }}
    # ... rest of config
```

**Update to**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}-config
data:
  tibemsd.conf: |
    server = {{ $instance.bootstrap.serverName }}
    
    {{- $instanceCertConfig := $instance.certificates | default dict }}
    {{- $certConfig := merge $instanceCertConfig $globalCertConfig }}
    {{- $instanceSslEnabled := and $sslEnabled (ne $certConfig.enabled false) }}
    
    {{- if $instanceSslEnabled }}
    # SSL Configuration
    ssl_server_identity = {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.certFile | default "tls.crt" }}
    ssl_server_key = {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.keyFile | default "tls.key" }}
    {{- if $certConfig.caFile }}
    ssl_server_trusted = {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.caFile }}
    {{- end }}
    listen = ssl://0.0.0.0:{{ $instance.service.tcpPort }}
    ssl_cipher_suites = ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256
    ssl_require_client_cert = disabled
    {{- else }}
    # Plain TCP (no SSL)
    listen = tcp://0.0.0.0:{{ $instance.service.tcpPort }}
    {{- end }}
    
    # ... rest of tibco config
```

### 3. Add Init Containers Section

**Insert after `spec:` in StatefulSet** (around line 150):

```yaml
spec:
  serviceName: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}
  replicas: {{ $instance.replicaCount }}
  selector:
    matchLabels:
      {{- include "service-virtualization.selectorLabels" $ | nindent 6 }}
      app.kubernetes.io/component: tibco-{{ $instance.name }}
  template:
    metadata:
      labels:
        {{- include "service-virtualization.selectorLabels" $ | nindent 8 }}
        app.kubernetes.io/component: tibco-{{ $instance.name }}
      annotations:
        checksum/config: {{ include (print $.Template.BasePath "/optional-services/tibco.yaml") $ | sha256sum }}
    spec:
      {{- if $.Values.serviceAccount.name }}
      serviceAccountName: {{ $.Values.serviceAccount.name }}
      {{- end }}
      
      # ADD THIS: Init containers for certificate download
      {{- $instanceCertConfig := $instance.certificates | default dict }}
      {{- $certConfig := merge $instanceCertConfig $globalCertConfig }}
      {{- $instanceSslEnabled := and $sslEnabled (ne $certConfig.enabled false) }}
      {{- if $instanceSslEnabled }}
      initContainers:
      {{- include "service-virtualization.certificateInitContainer" (dict "root" $ "certConfig" $certConfig "serviceName" (printf "tibco-%s" $instance.name)) | nindent 6 }}
      {{- end }}
      
      containers:
      # ... rest of spec
```

### 4. Add Certificate Volume Mounts in Container

**In the `containers:` section**, add volume mounts:

```yaml
containers:
- name: tibco-ems
  image: "{{ $instance.image.repository }}:{{ $instance.image.tag }}"
  imagePullPolicy: {{ $instance.image.pullPolicy }}
  
  ports:
  - containerPort: {{ $instance.service.tcpPort }}
    name: {{ if $instanceSslEnabled }}ems-ssl{{ else }}ems-tcp{{ end }}
    protocol: TCP
  - containerPort: {{ $instance.service.httpPort }}
    name: http
    protocol: TCP
  
  volumeMounts:
  # ADD THIS: Certificate volume mount
  {{- if $instanceSslEnabled }}
  {{- include "service-virtualization.certificateVolumeMount" $certConfig | nindent 2 }}
  {{- end }}
  
  # Existing volume mounts
  - name: config
    mountPath: /etc/tibco/config
    readOnly: true
  - name: data
    mountPath: /data
  - name: logs
    mountPath: /logs
  
  # ADD THIS: SSL environment variables
  env:
  - name: EMS_CONFIG_FILE
    value: /etc/tibco/config/tibemsd.conf
  {{- if $instanceSslEnabled }}
  - name: EMS_SSL_ENABLED
    value: "true"
  - name: EMS_SSL_CERT_PATH
    value: {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.certFile | default "tls.crt" }}
  - name: EMS_SSL_KEY_PATH
    value: {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.keyFile | default "tls.key" }}
  {{- end }}
  {{- if $instance.auth }}
  - name: EMS_ADMIN_USER
    value: {{ $instance.auth.username | quote }}
  - name: EMS_ADMIN_PASSWORD
    value: {{ $instance.auth.password | quote }}
  {{- end }}
  # ... rest of env vars
```

### 5. Add Certificate Volumes Section

**At the end of the pod spec**, update volumes:

```yaml
volumes:
# ADD THIS: Certificate volumes
{{- if $instanceSslEnabled }}
{{- include "service-virtualization.certificateVolume" (dict "root" $ "certConfig" $certConfig "serviceName" (printf "tibco-%s" $instance.name)) | nindent 6 }}
{{- include "service-virtualization.apiAuthVolume" $certConfig | nindent 6 }}
{{- end }}

# ADD THIS: Embedded certificate secret (if method=embedded)
{{- include "service-virtualization.embeddedCertSecret" (dict "root" $ "certConfig" $certConfig "serviceName" (printf "tibco-%s" $instance.name)) }}

# Existing volumes
- name: config
  configMap:
    name: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}-config
- name: data
  {{- if $instance.persistence.dataSize }}
  persistentVolumeClaim:
    claimName: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}-data
  {{- else }}
  emptyDir: {}
  {{- end }}
- name: logs
  {{- if $instance.persistence.logsSize }}
  persistentVolumeClaim:
    claimName: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}-logs
  {{- else }}
  emptyDir: {}
  {{- end }}
```

---

## Complete Example: SSL-Enabled TIBCO ConfigMap

Here's what the complete ConfigMap looks like with SSL support:

```yaml
{{- range $index, $instance := .Values.tibco.instances }}
{{- if $instance.enabled }}

{{- $instanceCertConfig := $instance.certificates | default dict }}
{{- $certConfig := merge $instanceCertConfig $globalCertConfig }}
{{- $instanceSslEnabled := and $sslEnabled (ne $certConfig.enabled false) }}

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "service-virtualization.fullname" $ }}-tibco-{{ $instance.name }}-config
  namespace: {{ $.Release.Namespace }}
  labels:
    {{- include "service-virtualization.labels" $ | nindent 4 }}
    app.kubernetes.io/component: tibco-{{ $instance.name }}
data:
  tibemsd.conf: |
    # =============================================================================
    # TIBCO EMS Server Configuration
    # Server: {{ $instance.bootstrap.serverName }}
    # SSL Enabled: {{ $instanceSslEnabled }}
    # =============================================================================
    
    server = {{ $instance.bootstrap.serverName }}
    
    {{- if $instanceSslEnabled }}
    # SSL/TLS Configuration
    ssl_server_identity = {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.certFile | default "tls.crt" }}
    ssl_server_key = {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.keyFile | default "tls.key" }}
    {{- if $certConfig.caFile }}
    ssl_server_trusted = {{ $certConfig.mountPath | default "/etc/ssl/certs" }}/{{ $certConfig.caFile }}
    {{- end }}
    
    # SSL Listener
    listen = ssl://0.0.0.0:{{ $instance.service.tcpPort }}
    
    # SSL Cipher Suites (TLS 1.2/1.3 compatible)
    ssl_cipher_suites = ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256:TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256
    
    # SSL Settings
    ssl_require_client_cert = disabled
    ssl_expected_hostname = {{ $instance.bootstrap.serverName }}
    
    {{- else }}
    # Plain TCP Listener (no SSL)
    listen = tcp://0.0.0.0:{{ $instance.service.tcpPort }}
    {{- end }}
    
    # HTTP Port (always plain HTTP for console)
    listen = http://0.0.0.0:{{ $instance.service.httpPort }}
    
    # Server Settings
    {{- if $instance.bootstrap.authEnabled }}
    authorization = enabled
    {{- else }}
    authorization = disabled
    {{- end }}
    
    {{- if $instance.bootstrap.allowDynamicDestinations }}
    create_queue = *
    create_topic = *
    {{- end }}
    
    # Client Connection Settings
    client_timeout = {{ $instance.bootstrap.clientTimeout | default "300" }}
    connection_cleanup_time = {{ $instance.bootstrap.connectionCleanupTime | default "600" }}
    
    # Statistics
    {{- if eq $instance.bootstrap.statistics "enabled" }}
    statistics = enabled
    {{- end }}
    
    # Store Configuration
    store = /data/ems-store
    
    # Logging
    logfile = /logs/tibemsd.log
    console_trace = DEFAULT

{{- end }}
{{- end }}
```

---

## Testing the Implementation

### Test 1: SSL Disabled (Default)

```bash
# Deploy with default (SSL disabled)
helm install test-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin \
  --create-namespace

# Check ConfigMap
kubectl get configmap -n margin -l app.kubernetes.io/component=tibco-serverA -o yaml

# Should see: listen = tcp://0.0.0.0:7222
```

### Test 2: SSL Enabled with Pre-Created Secret

```bash
# 1. Create certificate secret
openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout test-key.pem \
  -out test-cert.pem \
  -subj "/CN=*.margin.example.com"

kubectl create secret tls org-wide-cert \
  --cert=test-cert.pem \
  --key=test-key.pem \
  -n margin

# 2. Update values to enable SSL
# Edit teams/margin/optional-services/tibco-values.yaml:
#   global.certificates.enabled: true

# 3. Deploy
helm upgrade test-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# 4. Check ConfigMap
kubectl get configmap -n margin -l app.kubernetes.io/component=tibco-serverA -o yaml

# Should see: 
#   ssl_server_identity = /etc/ssl/certs/tls.crt
#   listen = ssl://0.0.0.0:7222

# 5. Check pod has certificate mounted
kubectl exec -it <pod-name> -n margin -- ls -la /etc/ssl/certs/
# Should see: tls.crt, tls.key

# 6. Test SSL connection
kubectl exec -it <pod-name> -n margin -- openssl s_client -connect localhost:7222
```

### Test 3: Per-Instance Certificate Override

```yaml
# teams/margin/optional-services/tibco-values.yaml
global:
  certificates:
    enabled: true
    method: secret
    secret:
      name: "org-wide-cert"

tibco:
  instances:
  - name: serverA
    # Uses global cert: org-wide-cert
  
  - name: serverB
    # Override with different cert
    certificates:
      method: secret
      secret:
        name: "serverB-specific-cert"
```

```bash
# Create serverB-specific cert
kubectl create secret tls serverB-specific-cert \
  --cert=serverB-cert.pem \
  --key=serverB-key.pem \
  -n margin

# Deploy
helm upgrade test-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# Verify serverA uses org-wide-cert
kubectl describe pod <serverA-pod> -n margin | grep -A 5 "Volumes:"
# Should show: secretName: org-wide-cert

# Verify serverB uses serverB-specific-cert
kubectl describe pod <serverB-pod> -n margin | grep -A 5 "Volumes:"
# Should show: secretName: serverB-specific-cert
```

### Test 4: API Download Method

```bash
# 1. Create API token secret
kubectl create secret generic cert-api-token \
  --from-literal=token="test-api-token-12345" \
  -n margin

# 2. Update values
# global.certificates.method: api
# global.certificates.api.url: "https://your-api.com"

# 3. Deploy
helm upgrade test-tibco ./helm-charts/service-virtualization \
  -f teams/margin/optional-services/tibco-values.yaml \
  --namespace margin

# 4. Check init container logs
kubectl logs <pod-name> -c download-certificate -n margin

# Should see:
#   === Downloading certificate for tibco-serverA ===
#   API URL: https://your-api.com/v1/certificates/download/tibco-serverA
#   Calling API...
#   ✅ Certificate downloaded successfully
```

---

## Validation Checklist

After implementing SSL support, verify:

- [ ] ConfigMap has correct SSL configuration when enabled
- [ ] ConfigMap has plain TCP configuration when disabled
- [ ] Init container appears when method=api
- [ ] Certificate volume is created based on method
- [ ] Certificate files are mounted in container
- [ ] SSL environment variables are set when enabled
- [ ] Pod starts successfully with SSL enabled
- [ ] TIBCO EMS accepts SSL connections
- [ ] Plain TCP still works when SSL disabled
- [ ] Per-instance certificate override works
- [ ] API authentication secret is mounted when using API method
- [ ] Embedded certificate creates secret correctly

---

## Troubleshooting

### Issue: Certificate Not Found

```bash
# Check if secret exists
kubectl get secret org-wide-cert -n margin

# Check if volume is created
kubectl describe pod <pod-name> -n margin | grep -A 10 "Volumes:"

# Check if certificate is mounted
kubectl exec -it <pod-name> -n margin -- ls -la /etc/ssl/certs/
```

### Issue: SSL Handshake Fails

```bash
# Check certificate in pod
kubectl exec -it <pod-name> -n margin -- \
  openssl x509 -in /etc/ssl/certs/tls.crt -noout -text

# Check TIBCO EMS logs
kubectl logs <pod-name> -n margin | grep -i ssl

# Test locally
kubectl exec -it <pod-name> -n margin -- \
  openssl s_client -connect localhost:7222 -showcerts
```

### Issue: Init Container Fails (API Method)

```bash
# Check init container logs
kubectl logs <pod-name> -c download-certificate -n margin

# Check API authentication
kubectl get secret cert-api-token -n margin -o yaml

# Test API manually
kubectl run curl --rm -it --image=curlimages/curl -- \
  curl -H "Authorization: Bearer <token>" \
  https://your-api.com/v1/certificates/download/tibco-serverA
```

---

## Summary

This implementation adds complete SSL/TLS support to TIBCO EMS with:

✅ **SSL Configuration**: Automatic SSL setup when enabled
✅ **3 Provisioning Methods**: secret, API, embedded
✅ **Per-Instance Override**: Different certs per instance
✅ **Backward Compatible**: SSL disabled by default
✅ **Production Ready**: Supports org-wide certificates
✅ **Dynamic Provisioning**: API-based certificate download
✅ **Security**: Proper permissions, read-only mounts

**The same pattern applies to IBM MQ, Kafka, and ActiveMQ!**

---

## Next Steps

1. Apply these changes to `tibco.yaml`
2. Test thoroughly (all 4 test scenarios)
3. Apply the same pattern to:
   - `ibmmq.yaml`
   - `kafka.yaml`
   - `activemq.yaml`
4. Create Gateway and VirtualService templates
5. Update documentation with real examples

---

## Pattern for Other Services

The pattern is identical for all services:

1. Add SSL check at template start
2. Update ConfigMap/config files with SSL settings
3. Add init containers for certificate download
4. Mount certificate volumes
5. Add SSL environment variables
6. Add certificate volumes to pod spec

**Service-Specific Changes**:
- TIBCO: `tibemsd.conf` SSL configuration
- IBM MQ: MQ keystore and channel configuration
- Kafka: `server.properties` SSL listeners
- ActiveMQ: `activemq.xml` SSL transport connector

All use the same helper functions!

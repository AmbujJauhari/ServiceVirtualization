{{/*
Expand the name of the chart.
*/}}
{{- define "service-virtualization.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "service-virtualization.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "service-virtualization.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "service-virtualization.labels" -}}
helm.sh/chart: {{ include "service-virtualization.chart" . }}
{{ include "service-virtualization.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "service-virtualization.selectorLabels" -}}
app.kubernetes.io/name: {{ include "service-virtualization.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Backend specific names and labels
*/}}
{{- define "service-virtualization.backend.fullname" -}}
{{ include "service-virtualization.fullname" . }}-backend
{{- end }}

{{- define "service-virtualization.backend.labels" -}}
{{ include "service-virtualization.labels" . }}
app.kubernetes.io/component: backend
{{- end }}

{{- define "service-virtualization.backend.selectorLabels" -}}
{{ include "service-virtualization.selectorLabels" . }}
app.kubernetes.io/component: backend
{{- end }}

{{/*
UI specific names and labels
*/}}
{{- define "service-virtualization.ui.fullname" -}}
{{ include "service-virtualization.fullname" . }}-ui
{{- end }}

{{- define "service-virtualization.ui.labels" -}}
{{ include "service-virtualization.labels" . }}
app.kubernetes.io/component: ui
{{- end }}

{{- define "service-virtualization.ui.selectorLabels" -}}
{{ include "service-virtualization.selectorLabels" . }}
app.kubernetes.io/component: ui
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "service-virtualization.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "service-virtualization.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Validation helper: Check if required value is provided
*/}}
{{- define "service-virtualization.required" -}}
{{- $value := index . 0 -}}
{{- $name := index . 1 -}}
{{- if or (not $value) (eq $value "") -}}
{{- fail (printf "ERROR: %s is required but not provided! Please set this value in your values.yaml" $name) -}}
{{- end -}}
{{- $value -}}
{{- end }}

{{/*
Validation helper: Check if required numeric value is provided
*/}}
{{- define "service-virtualization.requiredInt" -}}
{{- $value := index . 0 -}}
{{- $name := index . 1 -}}
{{- if or (not $value) (and (kindIs "string" $value) (eq $value "")) (and (kindIs "int" $value) (eq $value 0)) -}}
{{- fail (printf "ERROR: %s is required but not provided! Please set this numeric value in your values.yaml" $name) -}}
{{- end -}}
{{- $value -}}
{{- end }}


{{/*
Embedded certificate secret (if using embedded method)
Parameters: (dict "root" $ "certConfig" $certConfig "serviceName" $serviceName)
*/}}
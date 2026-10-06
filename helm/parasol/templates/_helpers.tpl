{{- define "parasol.mssqlName" -}}
{{- printf "%s-mssql" .Release.Name -}}
{{- end -}}

{{- define "parasol.mssqlDataName" -}}
{{- printf "%s-mssql-data" .Release.Name -}}
{{- end -}}

{{- define "parasol.appName" -}}
{{- printf "%s-app" .Release.Name -}}
{{- end -}}

{{- define "parasol.mssqlSecretName" -}}
{{- if .Values.mssql.existingSecret -}}
{{- .Values.mssql.existingSecret -}}
{{- else -}}
{{- printf "%s-mssql" .Release.Name -}}
{{- end -}}
{{- end -}}

{{- define "parasol.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: parasol-insurance
{{- end -}}

{{- define "parasol.mssqlLabels" -}}
app: {{ include "parasol.mssqlName" . }}
app.kubernetes.io/name: mssql
{{ include "parasol.labels" . }}
{{- end -}}

{{- define "parasol.appLabels" -}}
app: {{ include "parasol.appName" . }}
app.kubernetes.io/name: parasol-insurance
{{ include "parasol.labels" . }}
{{- end -}}

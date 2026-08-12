{{- define "bank-common.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "bank-common.databaseName" -}}
{{- printf "%s-db" .Chart.Name -}}
{{- end -}}

{{- define "bank-common.labels" -}}
app.kubernetes.io/name: {{ include "bank-common.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | default .Chart.Version | quote }}
app.kubernetes.io/part-of: my-bank
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end -}}

{{- define "bank-common.selectorLabels" -}}
app.kubernetes.io/name: {{ include "bank-common.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "bank-common.databaseLabels" -}}
app.kubernetes.io/name: {{ include "bank-common.databaseName" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
app.kubernetes.io/part-of: my-bank
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end -}}

{{- define "bank-common.databaseSelectorLabels" -}}
app.kubernetes.io/name: {{ include "bank-common.databaseName" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

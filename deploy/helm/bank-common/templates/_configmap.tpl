{{- define "bank-common.configmap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "bank-common.name" . }}
  labels:
    {{- include "bank-common.labels" . | nindent 4 }}
data:
  application.yml: |
    {{- tpl (toYaml .Values.config) . | nindent 4 }}
{{- end -}}

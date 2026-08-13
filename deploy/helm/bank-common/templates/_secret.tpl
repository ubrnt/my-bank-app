{{- define "bank-common.secret" -}}
{{- if and (not .Values.existingSecret) .Values.secrets -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ include "bank-common.name" . }}
  labels:
    {{- include "bank-common.labels" . | nindent 4 }}
type: Opaque
stringData:
  {{- range $key, $value := .Values.secrets }}
  {{ $key }}: {{ required (printf "%s is not set: pass it with --set or point existingSecret at a secret you created" $key) $value | quote }}
  {{- end }}
{{- end -}}
{{- end -}}

{{- define "bank-common.secretName" -}}
{{- default (include "bank-common.name" .) .Values.existingSecret -}}
{{- end -}}

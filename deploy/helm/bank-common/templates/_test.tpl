{{- define "bank-common.healthTest" -}}
apiVersion: v1
kind: Pod
metadata:
  name: {{ include "bank-common.name" . }}-health-test
  labels:
    {{- include "bank-common.labels" . | nindent 4 }}
  annotations:
    helm.sh/hook: test
    helm.sh/hook-delete-policy: before-hook-creation,hook-succeeded
spec:
  restartPolicy: Never
  containers:
    - name: health
      image: {{ (.Values.tests | default dict).image | default "curlimages/curl:8.11.1" }}
      command:
        - sh
        - -c
        - |
          set -e
          health=$(curl --fail --silent --max-time 5 \
            http://{{ include "bank-common.name" . }}:{{ .Values.service.port }}/actuator/health)
          echo "$health"
          echo "$health" | grep -q '"status":"UP"'
{{- end -}}

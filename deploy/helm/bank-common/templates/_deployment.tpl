{{- define "bank-common.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "bank-common.name" . }}
  labels:
    {{- include "bank-common.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "bank-common.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      annotations:
        checksum/config: {{ include "bank-common.configmap" . | sha256sum }}
      labels:
        {{- include "bank-common.selectorLabels" . | nindent 8 }}
    spec:
      containers:
        - name: {{ include "bank-common.name" . }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - name: http
              containerPort: {{ .Values.service.port }}
              protocol: TCP
          env:
            - name: SPRING_CONFIG_ADDITIONAL_LOCATION
              value: file:/config/
          {{- with .Values.secrets }}
          envFrom:
            - secretRef:
                name: {{ include "bank-common.secretName" $ }}
          {{- end }}
          {{- with .Values.startupProbe }}
          startupProbe:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          {{- with .Values.readinessProbe }}
          readinessProbe:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          {{- with .Values.livenessProbe }}
          livenessProbe:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          {{- with .Values.resources }}
          resources:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          volumeMounts:
            - name: config
              mountPath: /config
              readOnly: true
      volumes:
        - name: config
          configMap:
            name: {{ include "bank-common.name" . }}
{{- end -}}

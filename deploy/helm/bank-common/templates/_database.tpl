{{- define "bank-common.database" -}}
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: {{ include "bank-common.databaseName" . }}
  labels:
    {{- include "bank-common.databaseLabels" . | nindent 4 }}
spec:
  serviceName: {{ include "bank-common.databaseName" . }}
  replicas: 1
  selector:
    matchLabels:
      {{- include "bank-common.databaseSelectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "bank-common.databaseSelectorLabels" . | nindent 8 }}
    spec:
      containers:
        - name: postgres
          image: {{ .Values.database.image }}
          ports:
            - name: postgres
              containerPort: 5432
              protocol: TCP
          env:
            - name: POSTGRES_DB
              value: {{ .Values.database.name }}
            - name: POSTGRES_USER
              value: {{ .Values.database.user }}
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: {{ include "bank-common.secretName" . }}
                  key: {{ .Values.database.passwordSecretKey }}
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", {{ .Values.database.user | quote }}, "-d", {{ .Values.database.name | quote }}]
            periodSeconds: 5
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", {{ .Values.database.user | quote }}, "-d", {{ .Values.database.name | quote }}]
            initialDelaySeconds: 20
            periodSeconds: 20
          resources:
            {{- if .Values.database.resources }}
            {{- toYaml .Values.database.resources | nindent 12 }}
            {{- else }}
            {{- include "bank-common.databaseResources" . | nindent 12 }}
            {{- end }}
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
            - name: init
              mountPath: /docker-entrypoint-initdb.d
              readOnly: true
      volumes:
        - name: init
          configMap:
            name: {{ include "bank-common.databaseName" . }}-init
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: {{ .Values.database.storage }}
{{- end -}}

{{- define "bank-common.databaseService" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "bank-common.databaseName" . }}
  labels:
    {{- include "bank-common.databaseLabels" . | nindent 4 }}
spec:
  clusterIP: None
  selector:
    {{- include "bank-common.databaseSelectorLabels" . | nindent 4 }}
  ports:
    - name: postgres
      port: 5432
      targetPort: postgres
      protocol: TCP
{{- end -}}

{{- define "bank-common.databaseInit" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "bank-common.databaseName" . }}-init
  labels:
    {{- include "bank-common.databaseLabels" . | nindent 4 }}
data:
  01-schema.sql: |
    CREATE SCHEMA IF NOT EXISTS {{ .Values.database.schema }} AUTHORIZATION {{ .Values.database.user }};
    ALTER ROLE {{ .Values.database.user }} SET search_path = {{ .Values.database.schema }};
{{- end -}}

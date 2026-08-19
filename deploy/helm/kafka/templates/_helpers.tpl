{{- define "kafka.quorumVoters" -}}
{{- $name := include "bank-common.name" . -}}
{{- $headless := printf "%s-headless" $name -}}
{{- $port := .Values.service.controllerPort -}}
{{- $voters := list -}}
{{- range $i := until (int .Values.replicaCount) -}}
{{- $voters = append $voters (printf "%d@%s-%d.%s:%v" $i $name $i $headless $port) -}}
{{- end -}}
{{- join "," $voters -}}
{{- end -}}

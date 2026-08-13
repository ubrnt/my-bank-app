#!/bin/bash
set -e

cd "$(dirname "$0")/.."

eval $(minikube docker-env)

docker build -t my-bank/accounts-service:1.0.0      -f services/accounts-service/Dockerfile .
docker build -t my-bank/notifications-service:1.0.0 -f services/notifications-service/Dockerfile .
docker build -t my-bank/cash-service:1.0.0          -f services/cash-service/Dockerfile .
docker build -t my-bank/transfer-service:1.0.0      -f services/transfer-service/Dockerfile .

docker images | grep my-bank

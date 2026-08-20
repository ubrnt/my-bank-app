#!/bin/bash
set -e

cd "$(dirname "$0")"

helm dependency build kafka
helm dependency build zipkin
helm dependency build prometheus
helm dependency build accounts-service
helm dependency build notifications-service
helm dependency build cash-service
helm dependency build transfer-service
helm dependency build my-bank

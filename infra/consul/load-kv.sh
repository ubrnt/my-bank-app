#!/bin/sh
set -e

ADDR="http://consul:8500"
KV_DIR="/seed/kv"

load_dir() {
	dir="$1"
	profile="$2"
	for file in "$dir"/*.yml; do
		[ -e "$file" ] || continue
		app=$(basename "$file" .yml)
		consul kv put -http-addr="$ADDR" "config/$app${profile:+/$profile}/data" @"$file"
	done
}

load_dir "$KV_DIR" ""

for dir in "$KV_DIR"/*/; do
	[ -d "$dir" ] || continue
	load_dir "${dir%/}" "$(basename "$dir")"
done

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$ROOT_DIR/applications/food-ordering-app/src/main/java"
OUT_DIR="$ROOT_DIR/target/core-baseline-verification"
HARNESS="$ROOT_DIR/experiments/cluster-1.1/p01/CoreBaselineVerification.java"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

mapfile -t SOURCES < <(
  {
    find "$SRC_DIR/com/acme/foodordering/domain" -name '*.java' -print
    find "$SRC_DIR/com/acme/foodordering/application" -name '*.java' -print
    printf '%s\n' "$SRC_DIR/com/acme/foodordering/adapter/out/inmemory/InMemoryOrderRepository.java"
    printf '%s\n' "$HARNESS"
  } | sort
)

javac --release 21 -d "$OUT_DIR" "${SOURCES[@]}"
java -ea -cp "$OUT_DIR" CoreBaselineVerification

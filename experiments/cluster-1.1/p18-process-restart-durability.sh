#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

JAR="$(find applications/food-ordering-app/target -maxdepth 1 -type f -name 'food-ordering-app-*.jar' | head -n 1)"
if [[ -z "$JAR" || ! -f "$JAR" ]]; then
  echo "P18 ERROR: executable Spring Boot jar was not found. Run mvn verify first." >&2
  exit 1
fi

PORT="${P18_PORT:-18081}"
BASE_URL="http://127.0.0.1:$PORT"
LOG_DIR="applications/food-ordering-app/target/p18-process-restart"
mkdir -p "$LOG_DIR"

APP_PID=""

cleanup() {
  if [[ -n "${APP_PID:-}" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill -9 "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

wait_until_started() {
  local log_file="$1"
  local probe_id="123e4567-e89b-12d3-a456-426614179999"

  for _ in $(seq 1 120); do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      echo "P18 ERROR: application exited during startup." >&2
      cat "$log_file" >&2 || true
      exit 1
    fi

    local code
    code="$(curl -sS -o /dev/null -w '%{http_code}'       "$BASE_URL/orders/$probe_id" 2>/dev/null || true)"

    if [[ "$code" == "404" ]]; then
      return
    fi

    sleep 0.25
  done

  echo "P18 ERROR: application did not become ready." >&2
  cat "$log_file" >&2 || true
  exit 1
}

start_process() {
  local name="$1"
  local log_file="$LOG_DIR/$name.log"

  java -jar "$JAR" --server.port="$PORT" >"$log_file" 2>&1 &
  APP_PID=$!
  wait_until_started "$log_file"
  echo "P18 process started: name=$name pid=$APP_PID"
}

crash_process() {
  local crashed_pid="$APP_PID"
  kill -9 "$crashed_pid"
  wait "$crashed_pid" 2>/dev/null || true
  APP_PID=""
  echo "P18 process terminated with SIGKILL: pid=$crashed_pid"
  sleep 0.5
}

http_request() {
  local method="$1"
  local url="$2"
  local body_file="$3"
  local payload="${4:-}"

  if [[ -n "$payload" ]]; then
    curl -sS       -X "$method"       -H 'Content-Type: application/json'       -d "$payload"       -o "$body_file"       -w '%{http_code}'       "$url"
  else
    curl -sS       -X "$method"       -o "$body_file"       -w '%{http_code}'       "$url"
  fi
}

PROCESS_A_BODY="$LOG_DIR/process-a-response.json"
PROCESS_A_GET="$LOG_DIR/process-a-get.json"
PROCESS_A_CANCEL="$LOG_DIR/process-a-cancel.json"
PROCESS_B_GET="$LOG_DIR/process-b-get.json"

start_process "process-a"

PLACE_CODE="$(http_request POST "$BASE_URL/orders" "$PROCESS_A_BODY" '{
  "customerId": "customer-p18",
  "restaurantId": "restaurant-p18",
  "lines": [
    {
      "menuItemId": "burger-p18",
      "name": "Crash Test Burger",
      "quantity": 1,
      "unitPrice": 9.50
    }
  ]
}')"

if [[ "$PLACE_CODE" != "201" ]]; then
  echo "P18 ERROR: placing order returned HTTP $PLACE_CODE" >&2
  cat "$PROCESS_A_BODY" >&2
  exit 1
fi

ORDER_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' < "$PROCESS_A_BODY")"

GET_CODE="$(http_request GET "$BASE_URL/orders/$ORDER_ID" "$PROCESS_A_GET")"
if [[ "$GET_CODE" != "200" ]]; then
  echo "P18 ERROR: process A could not read placed order, HTTP $GET_CODE" >&2
  cat "$PROCESS_A_GET" >&2
  exit 1
fi

PLACED_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' < "$PROCESS_A_GET")"
if [[ "$PLACED_STATUS" != "PLACED" ]]; then
  echo "P18 ERROR: expected PLACED before cancellation, got $PLACED_STATUS" >&2
  exit 1
fi

CANCEL_CODE="$(http_request POST "$BASE_URL/orders/$ORDER_ID/cancel" "$PROCESS_A_CANCEL" '{
  "customerId": "customer-p18"
}')"
if [[ "$CANCEL_CODE" != "200" ]]; then
  echo "P18 ERROR: cancellation returned HTTP $CANCEL_CODE" >&2
  cat "$PROCESS_A_CANCEL" >&2
  exit 1
fi

CANCELLED_STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' < "$PROCESS_A_CANCEL")"
if [[ "$CANCELLED_STATUS" != "CANCELLED" ]]; then
  echo "P18 ERROR: expected CANCELLED before crash, got $CANCELLED_STATUS" >&2
  exit 1
fi

echo "P18 before crash: orderId=$ORDER_ID status=$CANCELLED_STATUS http=200"

crash_process

start_process "process-b"

AFTER_RESTART_CODE="$(http_request GET "$BASE_URL/orders/$ORDER_ID" "$PROCESS_B_GET")"
if [[ "$AFTER_RESTART_CODE" != "404" ]]; then
  echo "P18 ERROR: expected HTTP 404 after restart, got $AFTER_RESTART_CODE" >&2
  cat "$PROCESS_B_GET" >&2
  exit 1
fi

echo "P18 after restart: orderId=$ORDER_ID http=$AFTER_RESTART_CODE"
echo "P18 OBSERVED: state visible as CANCELLED in process A was absent from fresh process B."

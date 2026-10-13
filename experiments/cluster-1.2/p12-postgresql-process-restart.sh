#!/usr/bin/env bash
# C1.2-P12: positive counterfactual to C1.1-P18. Shared real PostgreSQL stays
# alive while two separate Java processes are started, killed and replaced.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
JAR="$(find applications/food-ordering-app/target -maxdepth 1 -type f -name 'food-ordering-app-*.jar' | head -n 1)"
if [[ -z "$JAR" || ! -f "$JAR" ]]; then echo "P12 ERROR: build jar first" >&2; exit 1; fi
: "${P12_JDBC_URL:?Set P12_JDBC_URL for the existing PostgreSQL service}"
: "${P12_DB_USERNAME:?Set P12_DB_USERNAME}"
: "${P12_DB_PASSWORD:?Set P12_DB_PASSWORD}"
PORT="${P12_PORT:-18082}"
BASE="http://127.0.0.1:$PORT"
DIR="applications/food-ordering-app/target/p12-process-restart"
mkdir -p "$DIR"
APP_PID=""
cleanup(){ if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then kill -9 "$APP_PID" 2>/dev/null || true; wait "$APP_PID" 2>/dev/null || true; fi; }
trap cleanup EXIT
start_app(){
  local label="$1" log="$DIR/$1.log"
  ORDER_REPOSITORY_BACKEND=postgres ORDER_JDBC_URL="$P12_JDBC_URL" \
    ORDER_DB_USERNAME="$P12_DB_USERNAME" ORDER_DB_PASSWORD="$P12_DB_PASSWORD" \
    java -jar "$JAR" --server.port="$PORT" >"$log" 2>&1 &
  APP_PID=$!
  for _ in $(seq 1 140); do
    if ! kill -0 "$APP_PID" 2>/dev/null; then echo "P12 ERROR: JVM exited early" >&2; cat "$log" >&2; exit 1; fi
    code="$(curl -sS --max-time 2 -o /dev/null -w '%{http_code}' "$BASE/orders/123e4567-e89b-12d3-a456-426614179998" 2>/dev/null || true)"
    if [[ "$code" == "404" ]]; then echo "P12 JVM started: $label pid=$APP_PID"; return; fi
    sleep 0.25
  done
  echo "P12 ERROR: JVM not ready" >&2; cat "$log" >&2; exit 1
}
request(){
  local method="$1" url="$2" file="$3" payload="${4:-}"
  if [[ -n "$payload" ]]; then
    curl -sS --max-time 8 -X "$method" -H 'Content-Type: application/json' -d "$payload" -o "$file" -w '%{http_code}' "$url"
  else
    curl -sS --max-time 8 -X "$method" -o "$file" -w '%{http_code}' "$url"
  fi
}
start_app A
placed="$DIR/placed.json" cancelled="$DIR/cancelled.json" restored="$DIR/restored.json"
place_code="$(request POST "$BASE/orders" "$placed" '{"customerId":"customer-p12-recovery","restaurantId":"restaurant-p12","lines":[{"menuItemId":"burger-p12","name":"Durable Burger","quantity":2,"unitPrice":9.50}]}')"
if [[ "$place_code" != "201" ]]; then echo "P12 ERROR: place HTTP $place_code" >&2; cat "$placed" >&2; exit 1; fi
order_id="$(python3 -c 'import json,sys;print(json.load(sys.stdin)["id"])' < "$placed")"
cancel_code="$(request POST "$BASE/orders/$order_id/cancel" "$cancelled" '{"customerId":"customer-p12-recovery"}')"
if [[ "$cancel_code" != "200" ]]; then echo "P12 ERROR: cancel HTTP $cancel_code" >&2; cat "$cancelled" >&2; exit 1; fi
python3 - "$cancelled" "$order_id" <<'PY'
import json,sys
data=json.load(open(sys.argv[1]))
assert data['id']==sys.argv[2]
assert data['status']=='CANCELLED'
assert str(data['total']) in ('19.0','19.00','19')
assert len(data['lines'])==1 and data['lines'][0]['quantity']==2
PY
echo "P12 BEFORE: acknowledged Order $order_id status=CANCELLED process=A"
kill -9 "$APP_PID"
wait "$APP_PID" 2>/dev/null || true
APP_PID=""
sleep 0.5
start_app B
get_code="$(request GET "$BASE/orders/$order_id" "$restored")"
if [[ "$get_code" != "200" ]]; then echo "P12 ERROR: recovery HTTP $get_code" >&2; cat "$restored" >&2; exit 1; fi
python3 - "$restored" "$order_id" <<'PY'
import json,sys
data=json.load(open(sys.argv[1]))
assert data['id']==sys.argv[2]
assert data['status']=='CANCELLED'
assert data['customerId']=='customer-p12-recovery'
assert data['restaurantId']=='restaurant-p12'
assert str(data['total']) in ('19.0','19.00','19')
assert len(data['lines'])==1
assert data['lines'][0]['menuItemId']=='burger-p12'
assert data['lines'][0]['quantity']==2
PY
echo "P12 OBSERVED: new JVM recovered same OrderId $order_id, CANCELLED, same customer/restaurant/line values, HTTP 200"
echo "P12 LIMIT: HTTP projection excludes workflow history; separate JDBC integration tests verify its reconstruction."

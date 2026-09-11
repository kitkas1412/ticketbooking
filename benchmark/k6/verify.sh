#!/usr/bin/env bash
#
# Post-run verification for k6/flash-sale.js.
#
# The k6 summary only proves the *admission gate* held. This script checks the
# durable state that actually matters: that Postgres never sold more tickets than
# exist, never sold the same ticket twice, drained the outbox, and that the Redis
# counter agrees with the database.
#
# Usage: ./k6/verify.sh [eventId]
#        (with no argument, uses the most recently created k6 event)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

DB_USER="${POSTGRES_USER:?POSTGRES_USER is not set (populate .env)}"
DB_NAME="${POSTGRES_DB:?POSTGRES_DB is not set (populate .env)}"

psql_q() {
  docker compose exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" -tAc "$1"
}

redis_q() {
  docker compose exec -T redis redis-cli "$@"
}

EVENT_ID="${1:-}"
if [[ -z "$EVENT_ID" ]]; then
  EVENT_ID="$(psql_q "SELECT id FROM event WHERE name LIKE 'k6 flash sale%' ORDER BY created_at DESC LIMIT 1;")"
  if [[ -z "$EVENT_ID" ]]; then
    echo "No k6 event found. Pass an eventId explicitly." >&2
    exit 1
  fi
  echo "Using most recent k6 event: $EVENT_ID"
fi

# Async settling is driven by a 2s outbox poll plus the consumer, so give the
# tail of the burst a moment to drain before snapshotting.
SETTLE_WAIT="${SETTLE_WAIT:-10}"
echo "Waiting ${SETTLE_WAIT}s for the async pipeline to drain..."
sleep "$SETTLE_WAIT"

TOTAL=$(psql_q       "SELECT total_tickets FROM event WHERE id = '$EVENT_ID';")
SOLD=$(psql_q        "SELECT count(*) FROM ticket WHERE event_id = '$EVENT_ID' AND status = 'SOLD';")
AVAILABLE=$(psql_q   "SELECT count(*) FROM ticket WHERE event_id = '$EVENT_ID' AND status = 'AVAILABLE';")
TICKET_ROWS=$(psql_q "SELECT count(*) FROM ticket WHERE event_id = '$EVENT_ID';")

CONFIRMED=$(psql_q "SELECT count(*) FROM orders WHERE event_id = '$EVENT_ID' AND status = 'CONFIRMED';")
PENDING=$(psql_q   "SELECT count(*) FROM orders WHERE event_id = '$EVENT_ID' AND status = 'PENDING';")
FAILED=$(psql_q    "SELECT count(*) FROM orders WHERE event_id = '$EVENT_ID' AND status = 'FAILED';")
ORDERS=$(psql_q    "SELECT count(*) FROM orders WHERE event_id = '$EVENT_ID';")

# Same ticket handed to two different orders. order_item.ticket_id is UNIQUE so
# this should be structurally impossible -- checked anyway, cheaply.
DOUBLE_SOLD=$(psql_q "
  SELECT count(*) FROM (
    SELECT oi.ticket_id FROM order_item oi
    JOIN ticket t ON t.id = oi.ticket_id
    WHERE t.event_id = '$EVENT_ID'
    GROUP BY oi.ticket_id HAVING count(*) > 1
  ) d;")

# A CONFIRMED order with no reserved ticket, or an order_item with no confirmed
# order -- either would mean the consumer's transaction is not actually atomic.
ORPHAN_ORDERS=$(psql_q "
  SELECT count(*) FROM orders o
  WHERE o.event_id = '$EVENT_ID' AND o.status = 'CONFIRMED'
    AND NOT EXISTS (SELECT 1 FROM order_item oi WHERE oi.order_id = o.id);")

DUP_IDEMPOTENCY=$(psql_q "
  SELECT count(*) FROM (
    SELECT idempotency_key FROM orders WHERE event_id = '$EVENT_ID'
    GROUP BY idempotency_key HAVING count(*) > 1
  ) d;")

OUTBOX_TOTAL=$(psql_q     "SELECT count(*) FROM outbox_events WHERE aggregate_type = 'ORDER';")
OUTBOX_UNSENT=$(psql_q    "SELECT count(*) FROM outbox_events WHERE published_at IS NULL;")

REDIS_COUNTER=$(redis_q GET "event:$EVENT_ID:tickets_available" | tr -d '\r')
# Normalise to a number so the numeric comparison below cannot blow up; an unset
# key is a real failure (the reconciler should have repopulated it), not a skip.
[[ "$REDIS_COUNTER" =~ ^-?[0-9]+$ ]] || REDIS_COUNTER=-1

pass=0
fail=0
check() { # check <condition-result> <label>
  if [[ "$1" == "ok" ]]; then
    printf '  \033[32mPASS\033[0m  %s\n' "$2"; pass=$((pass + 1))
  else
    printf '  \033[31mFAIL\033[0m  %s\n' "$2"; fail=$((fail + 1))
  fi
}
yn() { [[ "$1" -eq "$2" ]] && echo ok || echo no; }
le() { [[ "$1" -le "$2" ]] && echo ok || echo no; }

cat <<EOF

═══ durable-state verification ════════════════════════════════════
  event                  $EVENT_ID

  tickets                $TICKET_ROWS rows, declared total $TOTAL
    SOLD                 $SOLD
    AVAILABLE            $AVAILABLE

  orders                 $ORDERS
    CONFIRMED            $CONFIRMED
    PENDING              $PENDING
    FAILED               $FAILED

  outbox                 $OUTBOX_TOTAL rows, $OUTBOX_UNSENT still unpublished
  redis counter          $REDIS_COUNTER  (db says $AVAILABLE available)

  ─── invariants ───
EOF

check "$(le "$SOLD" "$TOTAL")"          "no oversell: SOLD ($SOLD) <= total ($TOTAL)"
check "$(yn "$DOUBLE_SOLD" 0)"          "no ticket sold twice ($DOUBLE_SOLD offenders)"
check "$(yn "$DUP_IDEMPOTENCY" 0)"      "no duplicate idempotency key ($DUP_IDEMPOTENCY offenders)"
check "$(yn "$ORPHAN_ORDERS" 0)"        "every CONFIRMED order has a ticket ($ORPHAN_ORDERS orphans)"
check "$(yn "$SOLD" "$CONFIRMED")"      "SOLD tickets ($SOLD) == CONFIRMED orders ($CONFIRMED)"
check "$(yn "$PENDING" 0)"              "outbox+consumer drained: 0 PENDING orders ($PENDING left)"
check "$(yn "$OUTBOX_UNSENT" 0)"        "outbox fully published ($OUTBOX_UNSENT unsent)"
check "$(yn "$REDIS_COUNTER" "$AVAILABLE")" "redis counter ($REDIS_COUNTER) == db available ($AVAILABLE)"

echo
if [[ "$fail" -eq 0 ]]; then
  printf '  \033[32mRESULT: %d/%d invariants hold. %d tickets, %d sold, 0 oversold.\033[0m\n' \
    "$pass" "$((pass + fail))" "$TOTAL" "$SOLD"
else
  printf '  \033[31mRESULT: %d/%d invariants FAILED.\033[0m\n' "$fail" "$((pass + fail))"
fi
echo "═══════════════════════════════════════════════════════════════════"
echo

exit $(( fail > 0 ? 1 : 0 ))

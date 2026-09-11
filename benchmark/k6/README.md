# Load tests

Concurrency tests for the flash-sale purchase path. The point is not throughput —
it is proving that the Redis admission gate, the idempotency keys, and the
transactional outbox actually hold when thousands of requests hit the last few
hundred tickets at once.

## Running

```bash
docker compose up -d --build     # 3 app instances behind Nginx
k6 run k6/flash-sale.js          # fire the burst
./k6/verify.sh                   # check the durable state afterwards
```

`flash-sale.js` creates its own event, so no fixture setup is needed.

### Knobs

| Env var | Default | Meaning |
|---|---|---|
| `BASE_URL` | `http://localhost` | Nginx front door |
| `TOTAL_TICKETS` | `500` | Tickets the event is created with |
| `REQUESTS` | `5000` | Total purchase attempts |
| `VUS` | `300` | Concurrent virtual users |
| `DUP_RATE` | `0.1` | Fraction of requests replaying a previous idempotency key |
| `PROBE_RATE` | `0.05` | Fraction of accepted orders polled to settlement |
| `EVENT_ID` | — | Reuse an existing event instead of creating one |

```bash
REQUESTS=20000 VUS=800 TOTAL_TICKETS=1000 k6 run k6/flash-sale.js
```

## What each script proves

`flash-sale.js` checks the **admission gate** — what the API handed out:

- `202` count never exceeds the number of tickets that exist
- every non-admitted request is cleanly rejected with `409`, never a `5xx`
- replayed idempotency keys return `200` with an empty body, consuming nothing
- a sampled slice of accepted orders reaches `CONFIRMED` through the outbox →
  RabbitMQ → consumer path

`verify.sh` checks the **durable state** — what Postgres and Redis actually
ended up holding. This is the half that matters, since the API could in
principle hand out correct responses while the database disagrees:

- `SOLD` tickets never exceed the event total
- no ticket appears in two order items, no idempotency key in two orders
- every `CONFIRMED` order has a reserved ticket
- `SOLD` tickets == `CONFIRMED` orders
- the outbox fully drained; no order stuck `PENDING`
- the Redis counter agrees with the database's `AVAILABLE` count

## Baseline result

500 tickets, 5,000 concurrent purchase attempts from 300 VUs, 3 app instances
behind Nginx, all on one laptop:

```
  202 accepted           500
  409 sold out           4008
  200 idempotent replay  492
  unexpected             0

  buy latency  p50 98.1ms  p95 1113.6ms
  throughput   804.6 req/s
  settle latency (async, end-to-end)  p50 4.5s

  8/8 durable invariants hold. 500 tickets, 500 sold, 0 oversold.
```

Latency here is bounded by running every component plus the load generator on a
single machine; treat the correctness numbers as the result, not the timings.

## Bugs this suite has caught

Worth keeping, because all three were invisible to functional testing:

1. **`OutboxEvent.payload` could not be written at all.** Hibernate only
   auto-detects a JSON `FormatMapper` for Jackson 2 or JSON-B, and Spring Boot 4
   ships Jackson 3 — every purchase failed at flush with `Could not find a
   FormatMapper`. Fixed by `config/HibernateJsonConfig`.
2. **The compensating rollback never ran.** `OrderServiceImpl.buyTicket` released
   the Redis claim from a `try/catch` inside a `@Transactional` method, so any
   failure at *commit* escaped it and the claimed ticket leaked until the
   reconciler next ran. Replaced with a `TransactionSynchronization` that fires
   on `afterCompletion`, which also releases the idempotency key so the client's
   retry is not mistaken for a duplicate.
3. **The outbox stopped after one page.** `OutboxEventPublisher` called its own
   `@Transactional` publish method directly, so the Spring proxy was bypassed and
   `publishedAt` was never persisted — the poller re-sent the same oldest 50 rows
   forever and no later event was ever delivered. Moved the publish into
   `OutboxEventRelay` so the call crosses a proxy boundary.

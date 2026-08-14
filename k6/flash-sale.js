// Flash-sale load test for the Ticket Booking System.
//
// Fires a burst of concurrent purchase requests at an event that has far fewer
// tickets than there are requests, and asserts the core invariant of the system:
//
//     number of 202 Accepted responses  <=  number of tickets that exist
//
// The Redis DECR admission gate is the only thing standing between the burst and
// an oversell, so this ratio is the whole point of the test. Everything that is
// not admitted must be cleanly rejected with 409 (sold out) -- never a 5xx, and
// never a silent success.
//
// Run:  k6 run k6/flash-sale.js
// Then: ./k6/verify.sh <eventId>     (checks the durable Postgres state)

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ---------------------------------------------------------------- config ----

const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const TOTAL_TICKETS = Number(__ENV.TOTAL_TICKETS || 500);
const REQUESTS = Number(__ENV.REQUESTS || 5000);
const VUS = Number(__ENV.VUS || 300);

// Fraction of requests that replay the VU's previous idempotency key, exercising
// the Redis SETIFABSENT dedupe path. These must come back 200 with a null body.
const DUP_RATE = Number(__ENV.DUP_RATE || 0.1);

// Fraction of accepted orders that get polled to completion, to measure how long
// the outbox -> RabbitMQ -> consumer path takes to settle an order. Kept low so
// the polling traffic does not distort the burst itself.
const PROBE_RATE = Number(__ENV.PROBE_RATE || 0.05);
const POLL_TIMEOUT_MS = Number(__ENV.POLL_TIMEOUT_MS || 20000);
const POLL_INTERVAL_S = Number(__ENV.POLL_INTERVAL_S || 0.25);

const RUN_ID = __ENV.RUN_ID || `k6-${Date.now()}`;

// 409 (sold out) and 404 are correct, expected outcomes here, not failures.
// Without this k6 would fold them into http_req_failed and the metric would be
// meaningless for this test.
http.setResponseCallback(http.expectedStatuses(200, 201, 202, 404, 409));

// --------------------------------------------------------------- metrics ----

const buyAccepted = new Counter('buy_accepted');    // 202 -> inventory claimed
const buyDuplicate = new Counter('buy_duplicate');  // 200 -> idempotent replay
const buySoldOut = new Counter('buy_sold_out');     // 409 -> cleanly rejected
const buyNotFound = new Counter('buy_not_found');   // 404
const buyUnexpected = new Counter('buy_unexpected'); // anything else == a bug

const buyLatency = new Trend('buy_latency', true);
const settleLatency = new Trend('order_settle_ms', true);

const settleConfirmed = new Counter('settle_confirmed');
const settleFailed = new Counter('settle_failed');
const settleTimeout = new Counter('settle_timeout');

export const options = {
  scenarios: {
    flash_sale: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: REQUESTS, // deterministic total, so the invariant math is exact
      maxDuration: '5m',
    },
  },
  thresholds: {
    // Hard failures: any unexpected status, or any request the server errored on.
    buy_unexpected: ['count == 0'],
    http_req_failed: ['rate == 0'],
    // The admission gate must never hand out more claims than there are tickets.
    buy_accepted: [`count <= ${TOTAL_TICKETS}`],
    // Sanity ceiling on the hot path; it does no synchronous DB write beyond the
    // order + outbox insert, so this should hold comfortably.
    'buy_latency': ['p(95) < 1000'],
  },
};

// ----------------------------------------------------------------- setup ----

export function setup() {
  if (__ENV.EVENT_ID) {
    console.log(`Reusing existing event ${__ENV.EVENT_ID}`);
    return { eventId: __ENV.EVENT_ID, totalTickets: TOTAL_TICKETS };
  }

  const now = new Date();
  const payload = {
    name: `k6 flash sale ${RUN_ID}`,
    description: 'Load-test event',
    totalTickets: TOTAL_TICKETS,
    ticketPrice: 49.99,
    saleStartAt: new Date(now.getTime() - 3600_000).toISOString(),
    saleEndAt: new Date(now.getTime() + 86_400_000).toISOString(),
  };

  const res = http.post(`${BASE_URL}/api/events`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '120s', // creating N ticket rows is a single big insert batch
  });

  if (res.status !== 201) {
    throw new Error(`setup: could not create event (${res.status}): ${res.body}`);
  }

  const eventId = res.json('data.eventId');
  console.log(`Created event ${eventId} with ${TOTAL_TICKETS} tickets`);
  console.log(`Firing ${REQUESTS} requests from ${VUS} VUs`);

  return { eventId, totalTickets: TOTAL_TICKETS };
}

// ------------------------------------------------------------------ test ----

// Per-VU state: k6 gives each VU its own module instance, so this is not shared.
let lastKey = null;

export default function (data) {
  const replay = lastKey !== null && Math.random() < DUP_RATE;
  const idempotencyKey = replay ? lastKey : `${RUN_ID}-v${__VU}-i${__ITER}`;
  if (!replay) lastKey = idempotencyKey;

  const res = http.post(
    `${BASE_URL}/api/events/${data.eventId}/buy`,
    JSON.stringify({ idempotencyKey }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'POST /api/events/:id/buy' },
    },
  );

  buyLatency.add(res.timings.duration);

  switch (res.status) {
    case 202: {
      buyAccepted.add(1);
      const orderId = res.json('data.orderId');
      check(res, {
        '202 carries an orderId': () => !!orderId,
        '202 order starts PENDING': (r) => r.json('data.status') === 'PENDING',
      });
      if (orderId && Math.random() < PROBE_RATE) {
        pollUntilSettled(orderId);
      }
      break;
    }
    case 200:
      // Idempotent replay: the key was already claimed, so the server does
      // nothing and returns an empty envelope.
      buyDuplicate.add(1);
      check(res, {
        'duplicate returns no data': (r) => r.json('data') === null,
        'duplicate was a replayed key': () => replay,
      });
      break;
    case 409:
      buySoldOut.add(1);
      break;
    case 404:
      buyNotFound.add(1);
      break;
    default:
      buyUnexpected.add(1);
      console.error(`unexpected ${res.status}: ${String(res.body).slice(0, 300)}`);
  }
}

// Polls GET /api/orders/:id until the async consumer settles the order.
//
// Note the response shape switches once the order is CONFIRMED: the endpoint
// then returns the *ticket* projection, whose `status` field is the TicketStatus
// (SOLD), not the OrderStatus. The presence of `ticketId` is what actually marks
// a settled-successful order.
function pollUntilSettled(orderId) {
  const startedAt = Date.now();
  const deadline = startedAt + POLL_TIMEOUT_MS;

  while (Date.now() < deadline) {
    sleep(POLL_INTERVAL_S);

    const res = http.get(`${BASE_URL}/api/orders/${orderId}`, {
      tags: { name: 'GET /api/orders/:id' },
    });
    if (res.status !== 200) continue;

    let body;
    try {
      body = res.json('data');
    } catch (e) {
      continue;
    }
    if (!body) continue;

    if (body.ticketId) {
      settleConfirmed.add(1);
      settleLatency.add(Date.now() - startedAt);
      return;
    }
    if (body.status === 'FAILED') {
      settleFailed.add(1);
      settleLatency.add(Date.now() - startedAt);
      return;
    }
  }

  settleTimeout.add(1);
}

// --------------------------------------------------------------- summary ----

export function handleSummary(data) {
  const n = (metric) => (data.metrics[metric] ? data.metrics[metric].values.count : 0);
  const t = (metric, stat) =>
    data.metrics[metric] ? data.metrics[metric].values[stat] : NaN;

  const accepted = n('buy_accepted');
  const duplicate = n('buy_duplicate');
  const soldOut = n('buy_sold_out');
  const notFound = n('buy_not_found');
  const unexpected = n('buy_unexpected');
  const total = accepted + duplicate + soldOut + notFound + unexpected;

  const ms = (v) => (Number.isFinite(v) ? `${v.toFixed(1)}ms` : 'n/a');

  const oversold = accepted > TOTAL_TICKETS;
  const unaccounted = total !== REQUESTS;

  const verdicts = [
    [!oversold, `accepted (${accepted}) <= tickets (${TOTAL_TICKETS})`],
    [unexpected === 0, `no unexpected statuses (${unexpected})`],
    [!unaccounted, `every request accounted for (${total}/${REQUESTS})`],
    [n('settle_timeout') === 0, `no probed order timed out (${n('settle_timeout')})`],
  ];

  const lines = [
    '',
    '═══ flash-sale invariant report ═══════════════════════════════════',
    '',
    `  tickets on sale        ${TOTAL_TICKETS}`,
    `  requests fired         ${REQUESTS} from ${VUS} VUs`,
    '',
    `  202 accepted           ${accepted}`,
    `  409 sold out           ${soldOut}`,
    `  200 idempotent replay  ${duplicate}`,
    `  404 not found          ${notFound}`,
    `  unexpected             ${unexpected}`,
    '',
    `  buy latency  p50 ${ms(t('buy_latency', 'med'))}  p95 ${ms(
      t('buy_latency', 'p(95)'),
    )}  p99 ${ms(t('buy_latency', 'p(99)'))}`,
    `  throughput             ${
      data.metrics.http_reqs
        ? data.metrics.http_reqs.values.rate.toFixed(1)
        : 'n/a'
    } req/s (all endpoints)`,
    '',
    `  probed orders          ${n('settle_confirmed')} confirmed, ${n(
      'settle_failed',
    )} failed, ${n('settle_timeout')} timed out`,
    `  settle latency         p50 ${ms(t('order_settle_ms', 'med'))}  p95 ${ms(
      t('order_settle_ms', 'p(95)'),
    )}`,
    '',
    '  ─── invariants ───',
    ...verdicts.map(([ok, label]) => `  ${ok ? 'PASS' : 'FAIL'}  ${label}`),
    '',
    verdicts.every(([ok]) => ok)
      ? `  RESULT: no oversell. ${accepted} claims against ${TOTAL_TICKETS} tickets.`
      : '  RESULT: INVARIANT VIOLATED — see failures above.',
    '',
    '  Now confirm the durable state:  ./k6/verify.sh <eventId>',
    '═══════════════════════════════════════════════════════════════════',
    '',
  ];

  return {
    stdout: lines.join('\n'),
    'k6/summary.json': JSON.stringify(data, null, 2),
  };
}

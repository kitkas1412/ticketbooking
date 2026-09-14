// Kiểm thử tải đợt mở bán vé với số yêu cầu đồng thời lớn hơn số vé.
// Điều kiện cần kiểm tra: số phản hồi 202 không vượt quá tổng số vé phát hành.
// Chạy từ thư mục gốc: k6 run benchmark/k6/flash-sale.js
// Sau đó đối chiếu dữ liệu bằng benchmark/k6/verify.sh <eventId>.
// Kịch bản còn dùng import và đường dẫn API cũ; cần đồng bộ trước khi chạy.

import http from 'benchmark/k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'benchmark/k6/metrics';

// Cấu hình tải và địa chỉ ứng dụng.

const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const TOTAL_TICKETS = Number(__ENV.TOTAL_TICKETS || 500);
const REQUESTS = Number(__ENV.REQUESTS || 5000);
const VUS = Number(__ENV.VUS || 300);

// Tỷ lệ request dùng lại idempotency key của virtual user (VU) để kiểm tra deduplication.
// Kỳ vọng HTTP 200 với trường data bằng null.
const DUP_RATE = Number(__ENV.DUP_RATE || 0.1);

// Tỷ lệ đơn được truy vấn tới khi hoàn tất để đo độ trễ outbox → RabbitMQ → consumer.
// Giữ tỷ lệ thấp để lưu lượng tra cứu ít ảnh hưởng tới đợt đặt vé.
const PROBE_RATE = Number(__ENV.PROBE_RATE || 0.05);
const POLL_TIMEOUT_MS = Number(__ENV.POLL_TIMEOUT_MS || 20000);
const POLL_INTERVAL_S = Number(__ENV.POLL_INTERVAL_S || 0.25);

const RUN_ID = __ENV.RUN_ID || `k6-${Date.now()}`;

// Chấp nhận 404 và 409 như kết quả dự kiến để không tính vào chỉ số lỗi HTTP.
http.setResponseCallback(http.expectedStatuses(200, 201, 202, 404, 409));

// Các bộ đếm kết quả và phân bố độ trễ.

const buyAccepted = new Counter('buy_accepted');    // 202: đã giữ một suất trong tồn kho
const buyDuplicate = new Counter('buy_duplicate');  // 200: gửi lại khóa đã dùng
const buySoldOut = new Counter('buy_sold_out');     // 409: từ chối do hết vé
const buyNotFound = new Counter('buy_not_found');   // 404
const buyUnexpected = new Counter('buy_unexpected'); // mã khác: ngoài kết quả dự kiến

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
      iterations: REQUESTS, // cố định tổng lượt gọi để đối chiếu số lượng
      maxDuration: '5m',
    },
  },
  thresholds: {
    // Lỗi khi có mã phản hồi ngoài dự kiến hoặc yêu cầu HTTP thất bại.
    buy_unexpected: ['count == 0'],
    http_req_failed: ['rate == 0'],
    // Số suất được tiếp nhận không được vượt quá tồn kho ban đầu.
    buy_accepted: [`count <= ${TOTAL_TICKETS}`],
    // Ngưỡng độ trễ p95 của bước tiếp nhận; thao tác cấp vé được xử lý bất đồng bộ.
    'buy_latency': ['p(95) < 1000'],
  },
};

// Chuẩn bị sự kiện dùng cho toàn bộ lượt kiểm thử.

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
    timeout: '120s', // chờ tạo danh sách vé ban đầu
  });

  if (res.status !== 201) {
    throw new Error(`setup: could not create event (${res.status}): ${res.body}`);
  }

  const eventId = res.json('data.eventId');
  console.log(`Created event ${eventId} with ${TOTAL_TICKETS} tickets`);
  console.log(`Firing ${REQUESTS} requests from ${VUS} VUs`);

  return { eventId, totalTickets: TOTAL_TICKETS };
}

// Gửi request mua vé.

// Mỗi VU có state riêng; lastKey không được chia sẻ giữa các VU.
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
      // Idempotency key đã tồn tại nên server bỏ qua request và trả data=null.
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

// Poll trạng thái đơn tới khi xử lý xong hoặc timeout.
// Kịch bản kỳ vọng đơn thành công trả DTO vé: ticketId là dấu hiệu hoàn tất,
// còn status khi đó là trạng thái vé SOLD. API tra cứu hiện đang bị comment trong mã Java.
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

// Tổng hợp chỉ số và kết quả các điều kiện kiểm tra.

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

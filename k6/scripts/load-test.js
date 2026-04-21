/**
 * Load Test — 혼합 트래픽 시뮬레이션
 * 목적: 현실적인 사용 패턴(조회 70% / 로그인 20% / 입찰 10%)으로 일반 부하 성능 측정
 * 실행: docker compose -f docker-compose.k6.yml up --build  (기본값)
 *   또는: docker compose -f docker-compose.k6.yml run --rm k6 \
 *           run --out influxdb=http://influxdb:8086/k6 /scripts/load-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { login, authHeaders, BASE_URL } from './helpers/auth.js';

const bidFailures = new Counter('bid_failures');

export const options = {
  scenarios: {
    // 70%: 경매 목록 + 상세 조회 (읽기 위주)
    browse: {
      executor: 'ramping-vus',
      stages: [
        { duration: '1m',  target: 21 },
        { duration: '3m',  target: 21 },
        { duration: '30s', target: 0  },
      ],
      exec: 'browseFn',
    },
    // 20%: 로그인 플로우 (인증 부하)
    login_flow: {
      executor: 'constant-arrival-rate',
      rate: 6,
      timeUnit: '1s',
      duration: '4m30s',
      preAllocatedVUs: 10,
      exec: 'loginFn',
    },
    // 10%: 입찰 (쓰기 부하, ramp-up 후 시작)
    bidding: {
      executor: 'constant-vus',
      vus: 3,
      duration: '3m30s',
      startTime: '1m',
      exec: 'biddingFn',
    },
  },
  thresholds: {
    'http_req_duration{name:get_auction_list}':   ['p(95)<300'],
    'http_req_duration{name:get_auction_detail}': ['p(95)<200'],
    'http_req_duration{name:login}':              ['p(95)<500'],
    'http_req_duration{name:create_bid}':         ['p(95)<800'],
    http_req_failed: ['rate<0.05'],
  },
};

export function setup() {
  // 10명의 buyer 토큰을 미리 확보
  const tokens = [];
  for (let i = 1; i <= 10; i++) {
    tokens.push(login(`buyer${i}@k6.test`, 'Test1234!'));
  }
  return { tokens };
}

// 경매 목록 + 상세 조회
export function browseFn() {
  const page = Math.floor(Math.random() * 2); // 0 or 1 페이지
  const listRes = http.get(
    `${BASE_URL}/api/auctions?page=${page}&size=10`,
    { tags: { name: 'get_auction_list' } }
  );
  check(listRes, { 'browse list: 200': (r) => r.status === 200 });

  sleep(0.3);

  const auctionId = (Math.floor(Math.random() * 20) + 1);
  const detailRes = http.get(
    `${BASE_URL}/api/auctions/${auctionId}`,
    { tags: { name: 'get_auction_detail' } }
  );
  check(detailRes, { 'browse detail: 200 or 404': (r) => r.status === 200 || r.status === 404 });

  sleep(1);
}

// 로그인 플로우
export function loginFn() {
  const i = (Math.floor(Math.random() * 10) + 1);
  const res = http.post(
    `${BASE_URL}/api/users/login`,
    JSON.stringify({ email: `buyer${i}@k6.test`, password: 'Test1234!' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  );
  check(res, { 'login: 200': (r) => r.status === 200 });
  sleep(0.5);
}

// 입찰 (VU별로 다른 경매에 분산)
export function biddingFn(data) {
  const vuIndex = (__VU - 1) % 10;
  const token = data.tokens[vuIndex];
  // VU별로 다른 경매 사용 (충돌 최소화)
  const auctionId = (__VU % 20) + 1;
  // VU × iteration 조합으로 단조 증가 보장 (bidUnit = 1000)
  const bidPrice = 10_000 + (__VU * 10_000) + (__ITER + 1) * 1_000;

  const res = http.post(
    `${BASE_URL}/api/auctions/bids`,
    JSON.stringify({ auctionId, bidPrice }),
    { ...authHeaders(token), tags: { name: 'create_bid' } }
  );

  check(res, {
    'bid: 200 or 400': (r) => r.status === 200 || r.status === 400,
  });
  if (res.status !== 200) {
    bidFailures.add(1);
  }

  sleep(2);
}

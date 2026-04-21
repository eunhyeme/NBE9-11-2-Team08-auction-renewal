/**
 * Smoke Test — 기본 동작 확인
 * 목적: 서버가 정상 동작하는지 최소 부하로 검증 (성능 측정 아님)
 * 실행: docker compose -f docker-compose.k6.yml run --rm k6 \
 *        run --out influxdb=http://influxdb:8086/k6 /scripts/smoke-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { login, authHeaders, BASE_URL } from './helpers/auth.js';

const bidFailures = new Counter('bid_failures');

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const token = login('buyer1@k6.test', 'Test1234!');

  // 경매 목록에서 첫 번째 경매 ID 확인
  const listRes = http.get(`${BASE_URL}/api/auctions`, { tags: { name: 'get_auction_list' } });
  const auctions = listRes.json('result.content');
  const auctionId = auctions && auctions.length > 0 ? auctions[0].auctionId : 1;
  const currentPrice = auctions && auctions.length > 0 ? auctions[0].currentPrice : 10000;
  const bidUnit = auctions && auctions.length > 0 ? auctions[0].bidUnit : 1000;

  return { token, auctionId, currentPrice, bidUnit };
}

export default function (data) {
  // 1. 경매 목록 조회
  const listRes = http.get(
    `${BASE_URL}/api/auctions?page=0&size=10`,
    { tags: { name: 'get_auction_list' } }
  );
  check(listRes, { 'auction list: status 200': (r) => r.status === 200 });

  sleep(0.5);

  // 2. 경매 상세 조회
  const detailRes = http.get(
    `${BASE_URL}/api/auctions/${data.auctionId}`,
    { tags: { name: 'get_auction_detail' } }
  );
  check(detailRes, { 'auction detail: status 200': (r) => r.status === 200 });

  sleep(0.5);

  // 3. 입찰 (VU * iteration 기반 단조 증가 가격)
  const bidPrice = data.currentPrice + data.bidUnit * (__VU * 100 + __ITER + 1);
  const bidRes = http.post(
    `${BASE_URL}/api/auctions/bids`,
    JSON.stringify({ auctionId: data.auctionId, bidPrice }),
    { ...authHeaders(data.token), tags: { name: 'create_bid' } }
  );

  const bidOk = check(bidRes, {
    'bid: status 200 or 400': (r) => r.status === 200 || r.status === 400,
  });
  if (bidRes.status !== 200) {
    bidFailures.add(1);
  }

  sleep(1);
}

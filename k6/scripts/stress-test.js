/**
 * Stress Test — 읽기 한계 탐색
 * 목적: VU를 점진적으로 늘려 응답시간이 급증하는 지점(병목)을 찾는다
 * 임계치 없음 — 관찰 목적이므로 Grafana에서 p95 곡선을 직접 확인
 * 실행: docker compose -f docker-compose.k6.yml run --rm k6 \
 *        run --out influxdb=http://influxdb:8086/k6 /scripts/stress-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from './helpers/auth.js';

export const options = {
  scenarios: {
    read_stress: {
      executor: 'ramping-vus',
      stages: [
        { duration: '1m', target: 10  },   // 워밍업
        { duration: '2m', target: 50  },   // 중간 부하
        { duration: '2m', target: 100 },   // 높은 부하
        { duration: '2m', target: 100 },   // 지속
        { duration: '1m', target: 0   },   // 쿨다운
      ],
    },
  },
  // 관찰 목적 — 임계치 미설정. Grafana p95 곡선으로 병목 구간 확인
};

export default function () {
  const page = Math.floor(Math.random() * 3);
  const size = 10;

  const res = http.get(
    `${BASE_URL}/api/auctions?page=${page}&size=${size}`,
    { tags: { name: 'get_auction_list' } }
  );
  check(res, { 'stress list: 200': (r) => r.status === 200 });

  sleep(0.5);

  // 랜덤 경매 상세도 간헐적으로 조회
  if (Math.random() < 0.3) {
    const auctionId = Math.floor(Math.random() * 20) + 1;
    const detailRes = http.get(
      `${BASE_URL}/api/auctions/${auctionId}`,
      { tags: { name: 'get_auction_detail' } }
    );
    check(detailRes, { 'stress detail: 200 or 404': (r) => r.status === 200 || r.status === 404 });
  }

  sleep(0.5);
}

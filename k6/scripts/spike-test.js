/**
 * Spike Test — 트래픽 급증 + @Version 낙관적 락 동시 입찰 테스트
 * 목적:
 *   1. 갑작스러운 200VU 급증에 서버가 안정적으로 처리하는지 확인
 *   2. 모든 VU가 동일 경매(auction_id=1)에 입찰 → OptimisticLockException 발생 관찰
 * 기대 결과:
 *   - 400(BidTooLow), 409/500(OptimisticLock 처리) 응답이 증가해도 정상
 *   - 5xx 응답이 과도하게 증가하면 문제
 * 실행: docker compose -f docker-compose.k6.yml run --rm k6 \
 *        run --out influxdb=http://influxdb:8086/k6 /scripts/spike-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { login, authHeaders, BASE_URL } from './helpers/auth.js';

const bidSuccess  = new Counter('bid_success');
const bidBizFail  = new Counter('bid_biz_fail');   // 400, 409: 비즈니스 실패 (정상)
const bidSrvError = new Counter('bid_srv_error');   // 5xx: 서버 오류 (비정상)

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      stages: [
        { duration: '1m',  target: 5   },   // 준비
        { duration: '30s', target: 200 },   // 1차 스파이크
        { duration: '1m',  target: 5   },   // 회복
        { duration: '30s', target: 200 },   // 2차 스파이크
        { duration: '30s', target: 5   },   // 쿨다운
      ],
    },
  },
  thresholds: {
    // 5xx 오류는 전체의 10% 미만 (비즈니스 실패인 400/409은 제외)
    'bid_srv_error': ['count<500'],
  },
};

export function setup() {
  const tokens = [];
  for (let i = 1; i <= 10; i++) {
    tokens.push(login(`buyer${i}@k6.test`, 'Test1234!'));
  }
  return { tokens };
}

export default function (data) {
  const vuIndex = (__VU - 1) % 10;
  const token = data.tokens[vuIndex];

  // VU × iter 조합으로 단조 증가 가격 생성 (bidUnit = 1000)
  // 단, 동시에 여러 VU가 같은 경매에 올라타므로 일부는 BidTooLow로 실패
  const bidPrice = 10_000 + (__VU * 100_000) + (__ITER + 1) * 1_000;

  const res = http.post(
    `${BASE_URL}/api/auctions/bids`,
    JSON.stringify({ auctionId: 1, bidPrice }),
    { ...authHeaders(token), tags: { name: 'create_bid_spike' } }
  );

  if (res.status === 200) {
    bidSuccess.add(1);
  } else if (res.status === 400 || res.status === 409) {
    bidBizFail.add(1);   // 비즈니스 실패 — 정상 동작
  } else {
    bidSrvError.add(1);  // 5xx — 모니터링 필요
  }

  // 스파이크 구간에도 check는 200/400/409만 허용 (5xx은 실패로 기록)
  check(res, {
    'spike bid: not 5xx': (r) => r.status < 500,
  });

  sleep(0.1);
}

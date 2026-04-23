/**
 * Bid Concurrency Comparison Test
 *
 * 목적: 동일 경매에 다수 동시 입찰이 몰릴 때
 *       /bids (createBid) vs /bids/atomic (createBidAtomic) 의 에러율·정합성 비교
 *
 * 시나리오 구성:
 *   - legacy  : N명이 /bids        로 동시 입찰 (기존 방식)
 *   - atomic  : N명이 /bids/atomic 으로 동시 입찰 (원자 UPDATE 방식)
 *   각 시나리오는 별도 경매를 사용해 서로 간섭하지 않음
 *
 * 비교 지표:
 *   - 500 에러 발생 수    → legacy 에서만 발생해야 함 (정합성 깨질 때)
 *   - BID_TOO_LOW(400-10) → atomic 에서만 명확하게 발생 (정상 처리)
 *   - 최종 현재가 정합성  → teardown 에서 출력
 *
 * 전제:
 *   - dev 프로파일 서버 기동 (DataInitializer 실행 완료)
 *   - buyer1~N @test.com / 1234 계정 존재 (DataInitializer 기본 2명, 부족하면 사전 생성 필요)
 *   - ONGOING 경매 2개 이상 존재
 *
 * 실행:
 *   k6 run k6/scripts/bid-concurrency-test.js
 *   k6 run k6/scripts/bid-concurrency-test.js --env VUS=10 --env DURATION=60s
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { login, authHeaders, BASE_URL } from './helpers/auth.js';

// ── 환경변수 ────────────────────────────────────────────────────
const VUS = parseInt(__ENV.VUS || '20');    // 최대 동시 VU 수 (버퍼)
const RATE = parseInt(__ENV.RATE || '5');   // 초당 요청 수 (양쪽 동일)
const DURATION = __ENV.DURATION || '60s';   // 시나리오 지속 시간

// ── 커스텀 메트릭 (legacy / atomic 각각 집계) ────────────────────
const legacySuccess  = new Counter('legacy_success');
const legacyTooLow   = new Counter('legacy_bid_too_low');
const legacy500      = new Counter('legacy_500_error');
const legacyOther4xx = new Counter('legacy_other_4xx');
const legacyTotal    = new Counter('legacy_total');

const atomicSuccess  = new Counter('atomic_success');
const atomicTooLow   = new Counter('atomic_bid_too_low');
const atomic500      = new Counter('atomic_500_error');
const atomicOther4xx = new Counter('atomic_other_4xx');
const atomicTotal    = new Counter('atomic_total');

const legacyDuration = new Trend('legacy_bid_duration_ms', true);
const atomicDuration = new Trend('atomic_bid_duration_ms', true);

const legacy500Rate      = new Rate('legacy_500_rate');
const atomic500Rate      = new Rate('atomic_500_rate');
// 유효 처리율: success + bid_too_low → 정상 처리된 요청 비율
const legacyValidRate    = new Rate('legacy_valid_rate');
const atomicValidRate    = new Rate('atomic_valid_rate');

// ── 옵션 ────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    legacy: {
      executor: 'constant-arrival-rate',
      rate: RATE,            // 초당 요청 수 고정
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: VUS,  // 응답 지연 시 추가 VU 확보용
      maxVUs: VUS * 2,
      exec: 'legacyFn',
      tags: { scenario: 'legacy' },
    },
    atomic: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: VUS,
      maxVUs: VUS * 2,
      exec: 'atomicFn',
      tags: { scenario: 'atomic' },
    },
  },
  thresholds: {
    'atomic_500_error': ['count==0'],
    'atomic_bid_duration_ms': ['p(95)<800'],
    // atomic 유효 처리율 100% 보장
    'atomic_valid_rate': ['rate==1'],
  },
};

// ── Setup: 토큰 + 경매 2개 확보 ─────────────────────────────────
export function setup() {
  // K6DataInitializer: buyer1~10@k6.test / Test1234! (10명 순환)
  const candidates = [];
  for (let i = 1; i <= 10; i++) {
    candidates.push({ email: `buyer${i}@k6.test`, password: 'Test1234!' });
  }
  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    const cred = candidates[i % candidates.length];
    tokens.push(login(cred.email, cred.password));
  }

  // 토큰 획득 실패 시 조기 종료
  const validTokens = tokens.filter(t => !!t);
  if (validTokens.length === 0) {
    console.error('[setup] 모든 로그인 실패. 서버가 구동 중인지, 계정이 존재하는지 확인하세요.');
    console.error(`[setup] BASE_URL=${BASE_URL}`);
    return {};
  }
  if (validTokens.length < tokens.length) {
    console.warn(`[setup] ${tokens.length - validTokens.length}개 로그인 실패. 유효 토큰: ${validTokens.length}개`);
  }

  // ONGOING 경매 2개 확보 (legacy용 / atomic용)
  const listRes = http.get(`${BASE_URL}/api/auctions?size=20`);
  console.log(`[setup] 경매 목록 status=${listRes.status} body=${listRes.body.substring(0, 200)}`);

  const content = listRes.json('result.content') || [];
  const ongoings = content.filter(a => a.status === 'ONGOING');

  if (ongoings.length < 2) {
    console.error(`[setup] ONGOING 경매가 ${ongoings.length}개밖에 없습니다. 최소 2개 필요.`);
    return {};
  }

  const legacyAuction = ongoings[0];
  const atomicAuction = ongoings[1];

  console.log(`[setup] legacy 경매: id=${legacyAuction.auctionId}, price=${legacyAuction.currentPrice}, unit=${legacyAuction.bidUnit}`);
  console.log(`[setup] atomic 경매: id=${atomicAuction.auctionId}, price=${atomicAuction.currentPrice}, unit=${atomicAuction.bidUnit}`);

  return {
    tokens: validTokens,
    legacy: { id: legacyAuction.auctionId, price: legacyAuction.currentPrice, unit: legacyAuction.bidUnit },
    atomic: { id: atomicAuction.auctionId, price: atomicAuction.currentPrice, unit: atomicAuction.bidUnit },
  };
}

// ── 시나리오: legacy (/bids) ─────────────────────────────────────
export function legacyFn(data) {
  if (!data.legacy || !data.legacy.id) return;

  const token = data.tokens[(__VU - 1) % data.tokens.length];
  // 모든 VU가 같은 ITER에서 동일한 가격 → 최대 충돌 재현
  const priceStep = data.legacy.unit * (__ITER + 1);
  const bidPrice = data.legacy.price + priceStep;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/auctions/bids`,
    JSON.stringify({ auctionId: data.legacy.id, bidPrice }),
    { ...authHeaders(token), tags: { name: 'legacy_bid' } }
  );
  legacyDuration.add(Date.now() - start);

  _record(res, 'legacy', bidPrice,
    legacySuccess, legacyTooLow, legacy500, legacyOther4xx, legacy500Rate, legacyTotal, legacyValidRate);
}

// ── 시나리오: atomic (/bids/atomic) ──────────────────────────────
export function atomicFn(data) {
  if (!data.atomic || !data.atomic.id) return;

  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const priceStep = data.atomic.unit * (__ITER + 1);
  const bidPrice = data.atomic.price + priceStep;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/auctions/bids/atomic`,
    JSON.stringify({ auctionId: data.atomic.id, bidPrice }),
    { ...authHeaders(token), tags: { name: 'atomic_bid' } }
  );
  atomicDuration.add(Date.now() - start);

  _record(res, 'atomic', bidPrice,
    atomicSuccess, atomicTooLow, atomic500, atomicOther4xx, atomic500Rate, atomicTotal, atomicValidRate);
}

// ── Teardown: 최종 경매 상태 비교 출력 ───────────────────────────
export function teardown(data) {
  if (!data.legacy || !data.legacy.id || !data.atomic || !data.atomic.id) return;

  const legacyRes = http.get(`${BASE_URL}/api/auctions/${data.legacy.id}`);
  const atomicRes = http.get(`${BASE_URL}/api/auctions/${data.atomic.id}`);
  const la = legacyRes.json('result');
  const aa = atomicRes.json('result');

  const legacyDelta = la.currentPrice - data.legacy.price;
  const atomicDelta = aa.currentPrice - data.atomic.price;
  // 단조 증가 여부: 최종가가 초기가보다 높아야 정상
  const legacyOk = legacyDelta > 0;
  const atomicOk = atomicDelta > 0;
  // legacy vs atomic 최종가 비교 (같은 초기가·bidUnit 경매 기준)
  const integrityNote = legacyDelta < atomicDelta
    ? `⚠️  legacy(+${legacyDelta}) < atomic(+${atomicDelta}) → dirty-check race로 낮은 가격이 덮어씀`
    : `✓  legacy(+${legacyDelta}) >= atomic(+${atomicDelta})`;

  // 유효 처리율 계산 (teardown 시점엔 Counter 값 직접 접근 불가 → API 재조회로 대체 불가)
  // CUSTOM 메트릭 출력은 k6 summary에서 legacy_valid_rate / atomic_valid_rate 로 확인
  console.log('');
  console.log('══════════════════════════════════════════════════════');
  console.log('  최종 경매 상태 비교  (RATE=' + RATE + '/s, VUS=' + VUS + ', bidUnit=' + data.legacy.unit + ')');
  console.log('──────────────────────────────────────────────────────');
  console.log(`  [legacy /bids]`);
  console.log(`    초기가: ${data.legacy.price}`);
  console.log(`    최종가: ${la.currentPrice}  변동: +${legacyDelta}  ${legacyOk ? '✓' : '⚠️ 입찰 미반영'}`);
  console.log(`    유효처리율: legacy_valid_rate 참조 (success + BID_TOO_LOW)`);
  console.log('──────────────────────────────────────────────────────');
  console.log(`  [atomic /bids/atomic]`);
  console.log(`    초기가: ${data.atomic.price}`);
  console.log(`    최종가: ${aa.currentPrice}  변동: +${atomicDelta}  ${atomicOk ? '✓' : '⚠️ 입찰 미반영'}`);
  console.log(`    유효처리율: atomic_valid_rate 참조 (success + BID_TOO_LOW)`);
  console.log('──────────────────────────────────────────────────────');
  console.log(`  정합성 판정: ${integrityNote}`);
  console.log('══════════════════════════════════════════════════════');
}

// ── 헬퍼 ────────────────────────────────────────────────────────
function _record(res, label, bidPrice, cSuccess, cTooLow, c500, cOther4xx, rate500, cTotal, rateValid) {
  cTotal.add(1);
  const is500 = res.status >= 500;
  rate500.add(is500 ? 1 : 0);

  check(res, { [`${label}: not 500`]: r => r.status < 500 });

  if (res.status === 200) {
    cSuccess.add(1);
    rateValid.add(1);  // 성공 → 유효
  } else if (res.status === 400) {
    const code = _code(res);
    if (code === '400-10') {
      cTooLow.add(1);
      rateValid.add(1);  // BID_TOO_LOW → 정상 처리, 유효
    } else {
      cOther4xx.add(1);
      rateValid.add(0);
    }
  } else if (is500) {
    c500.add(1);
    rateValid.add(0);  // 500 → 비정상
    console.warn(`[${label}] 💥 500 error bidPrice=${bidPrice} body=${res.body.substring(0, 200)}`);
  } else {
    cOther4xx.add(1);
    rateValid.add(0);
  }
}

function _code(res) {
  try { return res.json('code') || ''; }
  catch { return ''; }
}

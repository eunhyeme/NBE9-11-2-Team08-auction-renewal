import type { AuctionResponse } from "@/types/auction";
import type { OrderResponse } from "@/types/order";
import type { UserMeResponse } from "@/types/user";
import type { MembershipApplicationResponse } from "@/types/membership";
import type { Page } from "@/types/api";

// ─── 현재 시각 기준 상대 날짜 헬퍼 ─────────────────────────────────
const fromNow = (offsetMs: number) =>
  new Date(Date.now() + offsetMs).toISOString();

// ─── 경매 목 데이터 ──────────────────────────────────────────────────
export const MOCK_AUCTIONS: AuctionResponse[] = [
  {
    auctionId: 1,
    sellerId: 99,
    title: "Apple MacBook Pro 14인치 M3 Pro",
    imageUrl:
      "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400",
    description: "2024년 구매, 사용감 거의 없음. 충전기 포함.",
    startPrice: 1_500_000,
    currentPrice: 2_100_000,
    lastBidderId: 7,
    bidUnit: 50_000,
    status: "ONGOING",
    startedAt: fromNow(-2 * 3600_000),
    endedAt: fromNow(3 * 3600_000),
    version: 3,
  },
  {
    auctionId: 2,
    sellerId: 99,
    title: "Sony WH-1000XM5 노이즈 캔슬링 헤드폰",
    imageUrl:
      "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
    description: "6개월 사용. 박스, 케이스 포함.",
    startPrice: 200_000,
    currentPrice: 280_000,
    lastBidderId: 3,
    bidUnit: 10_000,
    status: "ONGOING",
    startedAt: fromNow(-1 * 3600_000),
    endedAt: fromNow(5 * 3600_000),
    version: 2,
  },
  {
    auctionId: 3,
    sellerId: 42,
    title: "닌텐도 스위치 OLED 화이트",
    imageUrl:
      "https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=400",
    description: "마리오카트 8 게임 포함.",
    startPrice: 250_000,
    currentPrice: 310_000,
    lastBidderId: 5,
    bidUnit: 5_000,
    status: "WON",
    startedAt: fromNow(-25 * 3600_000),
    endedAt: fromNow(-1 * 3600_000),
    version: 5,
  },
  {
    auctionId: 4,
    sellerId: 42,
    title: "삼성 갤럭시 S24 Ultra 티타늄 블랙",
    imageUrl:
      "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400",
    description: "미개봉 새 제품. 통신사 미약정.",
    startPrice: 900_000,
    currentPrice: 900_000,
    lastBidderId: null,
    bidUnit: 20_000,
    status: "UNSOLD",
    startedAt: fromNow(-48 * 3600_000),
    endedAt: fromNow(-24 * 3600_000),
    version: 0,
  },
  {
    auctionId: 5,
    sellerId: 15,
    title: "다이슨 V15 디텍트 청소기",
    imageUrl:
      "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400",
    description: "1년 사용. 흡입력 완벽. 필터 교체 완료.",
    startPrice: 300_000,
    currentPrice: 420_000,
    lastBidderId: 8,
    bidUnit: 20_000,
    status: "ONGOING",
    startedAt: fromNow(-30 * 60_000),
    endedAt: fromNow(8 * 3600_000),
    version: 1,
  },
  {
    auctionId: 6,
    sellerId: 33,
    title: "레고 테크닉 포르쉐 911 GT3 RS 42056",
    imageUrl:
      "https://images.unsplash.com/photo-1585366119957-e9730b6d0f60?w=400",
    description: "조립 완료 상태. 박스·설명서 있음.",
    startPrice: 80_000,
    currentPrice: 135_000,
    lastBidderId: 2,
    bidUnit: 5_000,
    status: "ONGOING",
    startedAt: fromNow(-3 * 3600_000),
    endedAt: fromNow(12 * 3600_000),
    version: 4,
  },
];

export const MOCK_AUCTIONS_PAGE: Page<AuctionResponse> = {
  content: MOCK_AUCTIONS,
  totalElements: MOCK_AUCTIONS.length,
  totalPages: 1,
  number: 0,
  size: 12,
  first: true,
  last: true,
};

// ─── 주문 목 데이터 ──────────────────────────────────────────────────
export const MOCK_ORDER: OrderResponse = {
  orderId: 101,
  auctionId: 3,
  auctionTitle: "닌텐도 스위치 OLED 화이트",
  auctionImageUrl:
    "https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=400",
  bidderId: 5,
  sellerId: 42,
  finalPrice: 310_000,
  status: "AWAITING_PAYMENT",
  paymentDeadline: fromNow(24 * 3600_000),
  paidAt: null,
  shippingAddress: "서울특별시 강남구 테헤란로 123, 101호",
  trackingNumber: null,
  createdAt: fromNow(-2 * 3600_000),
  deliveryDeadline: null,
};

export const MOCK_ORDERS: OrderResponse[] = [
  MOCK_ORDER,
  {
    orderId: 102,
    auctionId: 1,
    auctionTitle: "Apple MacBook Pro 14인치 M3 Pro",
    auctionImageUrl:
      "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400",
    bidderId: 5,
    sellerId: 99,
    finalPrice: 2_100_000,
    status: "DELIVERING",
    paymentDeadline: fromNow(-20 * 3600_000),
    paidAt: fromNow(-18 * 3600_000),
    shippingAddress: "서울특별시 강남구 테헤란로 123, 101호",
    trackingNumber: "CJ대한통운 123456789012",
    createdAt: fromNow(-22 * 3600_000),
    deliveryDeadline: fromNow(48 * 3600_000),
  },
  {
    orderId: 103,
    auctionId: 2,
    auctionTitle: "Sony WH-1000XM5 노이즈 캔슬링 헤드폰",
    auctionImageUrl:
      "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
    bidderId: 5,
    sellerId: 99,
    finalPrice: 280_000,
    status: "PAID",
    paymentDeadline: fromNow(-45 * 3600_000),
    paidAt: fromNow(-44 * 3600_000),
    shippingAddress: "서울특별시 강남구 테헤란로 123, 101호",
    trackingNumber: null,
    createdAt: fromNow(-48 * 3600_000),
    deliveryDeadline: null,
  },
];

// ─── 사용자 목 데이터 ────────────────────────────────────────────────
export const MOCK_ME: UserMeResponse = {
  userId: 5,
  email: "demo@example.com",
  nickname: "데모유저",
  address: "서울특별시 강남구 테헤란로 123, 101호",
  permissions: ["SELLER"],
  role: "ROLE_USER",
};

// ─── 판매자 인증 신청 목 데이터 ──────────────────────────────────────
export const MOCK_MEMBERSHIP_APPLICATIONS: MembershipApplicationResponse[] = [
  {
    id: 1,
    userId: 10,
    nickname: "판매왕김씨",
    status: "PENDING",
    reason: "중고 물품 판매 목적",
    requestedAt: fromNow(-3 * 3600_000),
    reviewedAt: null,
  },
  {
    id: 2,
    userId: 11,
    nickname: "플리마켓러",
    status: "APPROVED",
    reason: "수집품 판매",
    requestedAt: fromNow(-48 * 3600_000),
    reviewedAt: fromNow(-24 * 3600_000),
  },
  {
    id: 3,
    userId: 12,
    nickname: "전자기기마니아",
    status: "PENDING",
    reason: "IT 기기 전문 판매 예정",
    requestedAt: fromNow(-1 * 3600_000),
    reviewedAt: null,
  },
  {
    id: 4,
    userId: 13,
    nickname: "빈티지수집가",
    status: "REJECTED",
    reason: null,
    requestedAt: fromNow(-72 * 3600_000),
    reviewedAt: fromNow(-60 * 3600_000),
  },
];

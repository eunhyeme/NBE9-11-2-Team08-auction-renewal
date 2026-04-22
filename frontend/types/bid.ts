import type { AuctionStatus } from "./auction";

export interface BidResponse {
  auctionId: number;
  bidPrice: number;
  bidderId: number;
  bidedAt: string;
  nickname: string;
}

export interface MyBidResponse {
  bidId: number;
  auctionId: number;
  auctionTitle: string;
  auctionImageUrl: string | null;
  myBidPrice: number;
  currentPrice: number;
  auctionStatus: AuctionStatus;
  sellerId: number;
  sellerNickname: string;
  bidAt: string;
  auctionEndedAt: string;
  lastBidderId: number | null;
  isWinning: boolean;
}

export interface PlaceBidRequest {
  auctionId: number;
  bidderId: number;
  bidPrice: number;
}

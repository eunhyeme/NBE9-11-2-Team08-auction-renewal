import type { AuctionStatus } from "./auction";

export interface BidResponse {
  bidId: number;
  bidderId: number;
  auctionId: number;
  bidPrice: number;
  bidedAt: string;
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

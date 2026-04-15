export type AuctionStatus = "ONGOING" | "WON" | "UNSOLD" | "CANCELED";

export interface AuctionResponse {
  auctionId: number;
  sellerId: number;
  title: string;
  imageUrl: string | null;
  description: string | null;
  startPrice: number;
  currentPrice: number;
  lastBidderId: number | null;
  bidUnit: number;
  status: AuctionStatus;
  startedAt: string;
  endedAt: string;
  version: number;
}

export interface AuctionCreateRequest {
  title: string;
  imageUrl: string | null;
  description: string | null;
  startPrice: number;
  bidUnit: number;
  startedAt: string;
  endedAt: string;
}

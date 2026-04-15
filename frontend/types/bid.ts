export interface BidResponse {
  bidId: number;
  bidderId: number;
  auctionId: number;
  bidPrice: number;
  bidedAt: string;
}

export interface PlaceBidRequest {
  auctionId: number;
  bidderId: number;
  bidPrice: number;
}

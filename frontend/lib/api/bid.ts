import { apiFetch } from "./client";
import type { ApiResponse } from "@/types/api";
import type { BidResponse, MyBidResponse, PlaceBidRequest } from "@/types/bid";

export async function placeBid(data: PlaceBidRequest): Promise<BidResponse> {
  const res = await apiFetch.post<ApiResponse<BidResponse>>(
    "/api/auctions/bids",
    data,
  );
  return res.result;
}

export async function getMyBids(): Promise<MyBidResponse[]> {
  const res = await apiFetch.get<ApiResponse<MyBidResponse[]>>("/api/bids/me");
  return res.result;
}

export async function getMyBidHistory(auctionId: number): Promise<MyBidResponse[]> {
  const res = await apiFetch.get<ApiResponse<MyBidResponse[]>>(
    `/api/bids/me/${auctionId}`,
  );
  return res.result;
}

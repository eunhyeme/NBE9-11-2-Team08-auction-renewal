import { apiFetch } from "./client";
import type { ApiResponse } from "@/types/api";
import type { BidResponse, PlaceBidRequest } from "@/types/bid";

export async function placeBid(data: PlaceBidRequest): Promise<BidResponse> {
  const res = await apiFetch.post<ApiResponse<BidResponse>>(
    "/api/auctions/bids",
    data,
  );
  return res.result;
}

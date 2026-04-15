import { apiFetch } from "./client";
import type { ApiResponse, Page } from "@/types/api";
import type { AuctionCreateRequest, AuctionResponse } from "@/types/auction";

export async function getAuctions(
  page = 0,
  size = 10,
): Promise<Page<AuctionResponse>> {
  const res = await apiFetch.get<ApiResponse<Page<AuctionResponse>>>(
    `/api/auctions?page=${page}&size=${size}&sort=startedAt,desc`,
  );
  return res.result;
}

export async function getAuction(auctionId: number): Promise<AuctionResponse> {
  const res = await apiFetch.get<ApiResponse<AuctionResponse>>(
    `/api/auctions/${auctionId}`,
  );
  return res.result;
}

export async function createAuction(
  data: AuctionCreateRequest,
): Promise<AuctionResponse> {
  const res = await apiFetch.post<ApiResponse<AuctionResponse>>(
    "/api/auctions",
    data,
  );
  return res.result;
}

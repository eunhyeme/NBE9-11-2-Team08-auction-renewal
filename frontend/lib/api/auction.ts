import { apiFetch } from "./client";
import type { ApiResponse, Page } from "@/types/api";
import type { AuctionCreateRequest, AuctionResponse } from "@/types/auction";

export async function getAuctions(
  page = 0,
  size = 10,
  keyword?: string,
  sort = "startedAt,desc",
  minPrice?: number,
  maxPrice?: number,
  bluecheckOnly?: boolean,
): Promise<Page<AuctionResponse>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort,
  });
  if (keyword) params.set("keyword", keyword);
  if (minPrice !== undefined) params.set("minPrice", String(minPrice));
  if (maxPrice !== undefined) params.set("maxPrice", String(maxPrice));
  if (bluecheckOnly) params.set("bluecheckOnly", "true");
  const res = await apiFetch.get<ApiResponse<Page<AuctionResponse>>>(
    `/api/auctions?${params.toString()}`,
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

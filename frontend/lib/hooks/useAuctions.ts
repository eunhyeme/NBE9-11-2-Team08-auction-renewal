import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { getAuctions, getMyAuction, getMyAuctions } from "@/lib/api/auction";
import { MOCK_AUCTIONS, MOCK_AUCTIONS_PAGE } from "@/lib/mocks/data";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useAuctions(
  page = 0,
  size = 10,
  keyword?: string,
  sort = "startedAt,desc",
  minPrice?: number,
  maxPrice?: number,
  statuses?: string[],
) {
  return useQuery({
    queryKey: ["auctions", page, size, keyword, sort, minPrice, maxPrice, statuses],
    queryFn: USE_MOCK
      ? () => Promise.resolve(MOCK_AUCTIONS_PAGE)
      : () => getAuctions(page, size, keyword, sort, minPrice, maxPrice, statuses),
    placeholderData: keepPreviousData,
  });
}

export function useMyAuctions(enabled = true) {
  return useQuery({
    queryKey: ["myAuctions"],
    queryFn: USE_MOCK ? () => Promise.resolve(MOCK_AUCTIONS) : getMyAuctions,
    enabled,
  });
}

export function useMyAuction(auctionId: number, enabled = true) {
  return useQuery({
    queryKey: ["myAuction", auctionId],
    queryFn: USE_MOCK
      ? () =>
          Promise.resolve(
            MOCK_AUCTIONS.find((a) => a.auctionId === auctionId) ??
              MOCK_AUCTIONS[0],
          )
      : () => getMyAuction(auctionId),
    enabled: enabled && !!auctionId,
  });
}

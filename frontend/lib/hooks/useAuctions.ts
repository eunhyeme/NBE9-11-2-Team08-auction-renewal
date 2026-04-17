import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { getAuctions } from "@/lib/api/auction";
import { MOCK_AUCTIONS_PAGE } from "@/lib/mocks/data";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useAuctions(
  page = 0,
  size = 10,
  keyword?: string,
  sort = "startedAt,desc",
  minPrice?: number,
  maxPrice?: number,
  bluecheckOnly?: boolean,
) {
  return useQuery({
    queryKey: ["auctions", page, size, keyword, sort, minPrice, maxPrice, bluecheckOnly],
    queryFn: USE_MOCK
      ? () => Promise.resolve(MOCK_AUCTIONS_PAGE)
      : () => getAuctions(page, size, keyword, sort, minPrice, maxPrice, bluecheckOnly),
    placeholderData: keepPreviousData,
  });
}

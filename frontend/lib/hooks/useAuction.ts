import { useQuery } from "@tanstack/react-query";
import { getAuction } from "@/lib/api/auction";
import { MOCK_AUCTIONS } from "@/lib/mocks/data";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useAuction(auctionId: number) {
  return useQuery({
    queryKey: ["auction", auctionId],
    queryFn: USE_MOCK
      ? () =>
          Promise.resolve(
            MOCK_AUCTIONS.find((a) => a.auctionId === auctionId) ??
              MOCK_AUCTIONS[0],
          )
      : () => getAuction(auctionId),
    enabled: !!auctionId,
  });
}

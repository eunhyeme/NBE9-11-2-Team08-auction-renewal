import { useQuery } from "@tanstack/react-query";
import { getMyBidHistory, getMyBids } from "@/lib/api/bid";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useMyBids(enabled = true) {
  return useQuery({
    queryKey: ["myBids"],
    queryFn: USE_MOCK ? () => Promise.resolve([]) : getMyBids,
    enabled,
  });
}

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { placeBid } from "@/lib/api/bid";
import type { PlaceBidRequest } from "@/types/bid";

export function usePlaceBid(auctionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PlaceBidRequest) => placeBid(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["auction", auctionId] });
    },
  });
}

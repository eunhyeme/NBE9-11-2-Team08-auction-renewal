import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAuction, createAuction } from "@/lib/api/auction";
import { MOCK_AUCTIONS } from "@/lib/mocks/data";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

/**
 * 경매 상세 조회 훅 (기존 기능 복구)
 */
export function useAuction(auctionId: number) {
  return useQuery({
    queryKey: ["auction", auctionId],
    queryFn: USE_MOCK
      ? () =>
          Promise.resolve(
            MOCK_AUCTIONS.find((a) => a.auctionId === auctionId) ??
            MOCK_AUCTIONS[0]
          )
      : () => getAuction(auctionId),
    enabled: !!auctionId,
  });
}

/**
 * 경매 등록 훅 (신규 기능)
 */
export function useCreateAuction() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createAuction,
    onSuccess: () => {
      // 등록 성공 시 경매 목록 데이터를 무효화하여 최신화
      queryClient.invalidateQueries({ queryKey: ["auctions"] });
    },
  });
}
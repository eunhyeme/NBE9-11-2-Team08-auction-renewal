import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAuction, createAuction, deleteAuction } from "@/lib/api/auction";
import { MOCK_AUCTIONS } from "@/lib/mocks/data";
import { toast } from "sonner";
import { useRouter } from "next/navigation";

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

/**
 * 경매 철회 커스텀 훅
 */
export function useDeleteAuction() {
  const queryClient = useQueryClient();
  const router = useRouter(); // 라우터 객체 생성

  return useMutation({
    mutationFn: (auctionId: number) => deleteAuction(auctionId),
    onSuccess: () => {
      // 1. 성공 메시지 출력
      toast.success("삭제가 완료되었습니다.");
      
      // 2. 경매 목록 캐시 무효화 (메인 페이지 갔을 때 최신 상태를 보여주기 위함)
      queryClient.invalidateQueries({ queryKey: ["auctions"] });
      
      // 3. 메인 페이지로 이동 (404 방지)
      router.push("/");
    },
    onError: (error: Error) => {
      // 백엔드에서 보낸 구체적인 에러 메시지(예: "입찰자가 있어 철회 불가")를 보여줌
      toast.error(error.message);
    },
  });
}

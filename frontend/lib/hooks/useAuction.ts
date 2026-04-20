import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAuction, createAuction, deleteAuction, updateAuction } from "@/lib/api/auction";
import { MOCK_AUCTIONS } from "@/lib/mocks/data";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { AuctionFormValues } from "../validations/auctionSchema";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

/**
 * 경매 상세 조회 훅
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
 * 경매 등록 훅
 * (등록 성공 시 상세 페이지 이동은 컴포넌트에서 결과 ID를 받아 처리하므로 invalidate만 수행)
 */
export function useCreateAuction() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createAuction,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["auctions"] });
    },
  });
}

/**
 * 경매 철회 커스텀 훅
 */
export function useDeleteAuction() {
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: (auctionId: number) => deleteAuction(auctionId),
    onSuccess: () => {
      toast.success("경매가 철회되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["auctions"] });
      router.push("/");
    },
    onError: (error: Error) => {
      toast.error(error.message);
    },
  });
}

/**
 * 경매 수정 커스텀 훅 (안전성 강화 버전)
 */
export function useUpdateAuction() {
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: ({ auctionId, data }: { auctionId: number; data: AuctionFormValues }) => 
      updateAuction(auctionId, data),
    
    onSuccess: (_, variables) => {
      // 💡 변수(variables)에 담긴 ID를 직접 꺼내 씁니다. 이게 제일 확실합니다.
      const id = variables.auctionId;
      
      toast.success("경매 정보가 성공적으로 수정되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["auction", id] });
      queryClient.invalidateQueries({ queryKey: ["auctions"] });
      
      // 💡 여기서 바로 이동!
      router.push(`/auctions/${id}`);
    },
    onError: (error: Error) => {
      toast.error(error.message || "수정 중 오류가 발생했습니다.");
    },
  });
}

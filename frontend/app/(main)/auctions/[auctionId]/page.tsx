"use client";

import { use } from "react";
import { AuctionDetailPanel } from "@/components/auction/AuctionDetailPanel";
import { BidPanel } from "@/components/auction/BidPanel";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { BackButton } from "@/components/common/BackButton";
import { useAuction, useDeleteAuction } from "@/lib/hooks/useAuction";
import { Button } from "@/components/ui/button";
import { useMe } from "@/lib/hooks/useMe";

interface AuctionDetailPageProps {
  params: Promise<{ auctionId: string }>;
}

export default function AuctionDetailPage({ params }: AuctionDetailPageProps) {
  const { auctionId } = use(params);
  const id = Number(auctionId);
  
  const { data: auction, isLoading, isError } = useAuction(id);
  const { data: me } = useMe();
  const { mutate: withdrawAuction, isPending } = useDeleteAuction();

  // 판매자 본인 여부 확인
  const isSeller = me?.userId === auction?.sellerId;

  const handleWithdraw = () => {
    if (confirm("정말로 이 경매를 철회하시겠습니까?\n입찰자가 있는 경우 철회가 불가능합니다.")) {
      withdrawAuction(id);
    }
  };

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto">
      <div className="flex flex-col gap-2">
        <BackButton href="/" label="경매 목록" />
        
        {/* 💡 판매자 본인일 때만 목록 버튼 밑에 철회 버튼 노출 */}
        {isSeller && (
          <Button 
            variant="destructive" 
            className="w-fit" // 목록 버튼과 정렬을 맞추기 위해 너비 조절
            onClick={handleWithdraw}
            disabled={isPending}
          >
            {isPending ? "철회 중..." : "경매 철회하기"}
          </Button>
        )}
      </div>

      {isLoading && (
        <div className="flex justify-center py-16">
          <LoadingSpinner />
        </div>
      )}

      {isError && (
        <ErrorMessage message="경매 정보를 불러오는 데 실패했습니다." />
      )}

      {auction && (
        <>
          <AuctionDetailPanel auction={auction} />
          <BidPanel auction={auction} />
        </>
      )}
    </div>
  );
}

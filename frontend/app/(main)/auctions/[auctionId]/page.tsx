"use client";

import { use, useState } from "react";
import { AuctionDetailPanel } from "@/components/auction/AuctionDetailPanel";
import { BidPanel } from "@/components/auction/BidPanel";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { BackButton } from "@/components/common/BackButton";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { Button } from "@/components/ui/button";
import { useAuction, useDeleteAuction } from "@/lib/hooks/useAuction";
import { useMyBidHistory } from "@/lib/hooks/useBids";
import { useMe } from "@/lib/hooks/useMe";
import { formatDate } from "@/lib/utils/formatDate";
import type { MyBidResponse } from "@/types/bid";
import { useRouter } from "next/navigation"; // 💡 페이지 이동을 위한 라우터 추가

interface AuctionDetailPageProps {
  params: Promise<{ auctionId: string }>;
}

export default function AuctionDetailPage({ params }: AuctionDetailPageProps) {
  const { auctionId } = use(params);
  const id = Number(auctionId);
  const router = useRouter(); // 라우터 초기화

  const { data: auction, isLoading, isError } = useAuction(id);
  const { data: me } = useMe();
  const { mutate: withdrawAuction, isPending } = useDeleteAuction();
  const { data: myBidHistory } = useMyBidHistory(id, !!me);

  // 판매자 본인 여부 확인
  const isSeller = me?.userId === auction?.sellerId;

  // 입찰 존재 여부 확인 (입찰이 있으면 수정/철회 제한)
  const hasBids = auction?.lastBidderId != null;

  const handleWithdraw = () => {
    if (confirm("정말로 이 경매를 철회하시겠습니까?\n입찰자가 있는 경우 철회가 불가능합니다.")) {
      withdrawAuction(id);
    }
  };

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto">
      <div className="flex flex-col gap-2">
        <BackButton href="/" label="경매 목록" />

        {/* 판매자 전용 버튼 그룹 */}
        {isSeller && (
          <div className="flex gap-2">
            {/* 💡 경매 수정하기: 연한 노란색 바탕에 진한 노란색 글씨 */}
            <Button
              className="w-fit bg-yellow-100 text-yellow-700 hover:bg-yellow-200 font-bold border-none"
              onClick={() => router.push(`/auctions/${id}/edit`)}
              disabled={hasBids}
            >
              경매 수정하기
            </Button>

            {/* 경매 철회하기: 기존 스타일 유지 (연한 빨간색 바탕에 진한 빨간색 글씨 느낌) */}
            <Button
              variant="destructive"
              className="w-fit font-bold"
              onClick={handleWithdraw}
              disabled={isPending || hasBids}
            >
              {isPending ? "철회 중..." : "경매 철회하기"}
            </Button>
          </div>
        )}

        {/* 입찰이 있을 경우 안내 메시지 */}
        {isSeller && hasBids && (
          <p className="text-xs text-destructive font-medium">입찰이 진행 중인 경매는 수정하거나 철회할 수 없습니다.</p>
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
          {myBidHistory && myBidHistory.length > 0 && (
            <MyBidHistoryPanel bids={myBidHistory} />
          )}
        </>
      )}
    </div>
  );
}

function MyBidHistoryPanel({ bids }: { bids: MyBidResponse[] }) {
  const [expanded, setExpanded] = useState(false);
  const hasMore = bids.length > 3;
  const visibleBids = expanded ? bids : bids.slice(0, 3);

  return (
    <div className="rounded-xl border bg-card p-5">
      <div className="mb-4">
        <h2 className="text-lg font-semibold">내 입찰 히스토리</h2>
        <p className="text-sm text-muted-foreground">
          이 경매에 내가 참여한 입찰 내역입니다.
        </p>
      </div>
      <div className="flex flex-col divide-y">
        {visibleBids.map((bid) => (
          <div
            key={bid.bidId}
            className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
          >
            <div className="flex flex-col gap-1">
              <span className="text-sm font-medium">
                {bid.isWinning ? "현재 최고 입찰" : "입찰"}
              </span>
              <span className="text-xs text-muted-foreground">
                {formatDate(bid.bidAt)}
              </span>
            </div>
            <PriceDisplay amount={bid.myBidPrice} />
          </div>
        ))}
      </div>
      {hasMore && (
        <Button
          variant="outline"
          size="sm"
          className="mt-4 w-full"
          onClick={() => setExpanded((v) => !v)}
        >
          {expanded ? "접기" : `더보기 (${bids.length - 3})`}
        </Button>
      )}
    </div>
  );
}

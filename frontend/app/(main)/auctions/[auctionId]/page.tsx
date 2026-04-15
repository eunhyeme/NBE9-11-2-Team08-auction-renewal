"use client";

import { use } from "react";
import { AuctionDetailPanel } from "@/components/auction/AuctionDetailPanel";
import { BidPanel } from "@/components/auction/BidPanel";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { BackButton } from "@/components/common/BackButton";
import { useAuction } from "@/lib/hooks/useAuction";

interface AuctionDetailPageProps {
  params: Promise<{ auctionId: string }>;
}

export default function AuctionDetailPage({ params }: AuctionDetailPageProps) {
  const { auctionId } = use(params);
  const id = Number(auctionId);
  const { data: auction, isLoading, isError } = useAuction(id);

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto">
      <BackButton href="/" label="경매 목록" />

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

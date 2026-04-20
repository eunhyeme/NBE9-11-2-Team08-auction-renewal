"use client";

import { use } from "react";
import Link from "next/link";

import { AuctionDetailPanel } from "@/components/auction/AuctionDetailPanel";
import { BackButton } from "@/components/common/BackButton";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { buttonVariants } from "@/components/ui/button";
import { useMyAuction } from "@/lib/hooks/useAuctions";
import { cn } from "@/lib/utils";

interface MyAuctionDetailPageProps {
  params: Promise<{ auctionId: string }>;
}

export default function MyAuctionDetailPage({ params }: MyAuctionDetailPageProps) {
  const { auctionId } = use(params);
  const id = Number(auctionId);
  const { data: auction, isLoading, isError } = useMyAuction(id);

  return (
    <div className="flex flex-col gap-6 max-w-5xl mx-auto">
      <BackButton href="/profile" label="마이페이지" />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">내 판매 경매 상세</h1>
          <p className="text-sm text-muted-foreground mt-1">
            내가 등록한 경매의 상세 정보입니다.
          </p>
        </div>
        <Link
          href={`/auctions/${id}`}
          className={cn(buttonVariants({ variant: "outline", size: "sm" }), "self-start")}
        >
          공개 상세 보기
        </Link>
      </div>

      {isLoading && (
        <div className="flex justify-center py-16">
          <LoadingSpinner />
        </div>
      )}

      {isError && (
        <ErrorMessage message="내 경매 정보를 불러오는 데 실패했습니다." />
      )}

      {auction && (
        <>
          <div className="rounded-xl border bg-card p-4">
            <p className="text-sm text-muted-foreground">
              판매자 전용 조회 화면입니다. 구매자에게 보이는 공개 상세는 오른쪽의
              공개 상세 보기에서 확인할 수 있습니다.
            </p>
          </div>
          <AuctionDetailPanel auction={auction} />
        </>
      )}
    </div>
  );
}

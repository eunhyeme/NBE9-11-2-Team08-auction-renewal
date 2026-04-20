"use client";

import { use } from "react";
import { useAuction } from "@/lib/hooks/useAuction";
import { useMe } from "@/lib/hooks/useMe";
import AuctionEditForm from "@/components/auction/AuctionEditForm"; // 아까 만든 index.tsx 호출
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { BackButton } from "@/components/common/BackButton";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

interface AuctionEditPageProps {
  params: Promise<{ auctionId: string }>;
}

export default function AuctionEditPage({ params }: AuctionEditPageProps) {
  const { auctionId } = use(params);
  const id = Number(auctionId);
  const router = useRouter();

  const { data: auction, isLoading, isError } = useAuction(id);
  const { data: me } = useMe();

  // 권한 및 수정 가능 여부 체크
  useEffect(() => {
    if (auction && me) {
      const isSeller = me.userId === auction.sellerId;
      const hasBids = auction.currentPrice > auction.startPrice;

      if (!isSeller) {
        alert("본인이 등록한 경매만 수정할 수 있습니다.");
        router.push(`/auctions/${id}`);
      } else if (hasBids) {
        alert("이미 입찰이 진행된 경매는 수정할 수 없습니다.");
        router.push(`/auctions/${id}`);
      }
    }
  }, [auction, me, id, router]);

  if (isLoading) return <div className="py-20 flex justify-center"><LoadingSpinner /></div>;
  if (isError || !auction) return <ErrorMessage message="경매 정보를 불러올 수 없습니다." />;

  return (
    <div className="max-w-5xl mx-auto flex flex-col gap-8 p-4">
      <div className="flex flex-col gap-2">
        <BackButton href={`/auctions/${id}`} label="상세 페이지로 돌아가기" />
        <h1 className="text-3xl font-bold mt-4">경매 정보 수정</h1>
        <p className="text-muted-foreground">경매 내용을 확인하고 수정해 주세요.</p>
      </div>

      <div className="bg-white rounded-2xl border p-8 shadow-sm">
        {/* 아까 폴더 기반 index.tsx로 구현한 폼 호출 */}
        <AuctionEditForm auction={auction} />
      </div>
    </div>
  );
}

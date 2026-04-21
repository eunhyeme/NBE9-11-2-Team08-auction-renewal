"use client";

import { use } from "react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { AuctionDetailPanel } from "@/components/auction/AuctionDetailPanel";
import { BidPanel } from "@/components/auction/BidPanel";
import { BidHistoryChart } from "@/components/auction/BidHistoryChart/BidHistoryChart";

import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { BackButton } from "@/components/common/BackButton";
import { Button } from "@/components/ui/button";

import { useAuction, useDeleteAuction } from "@/lib/hooks/useAuction";
import { useMe } from "@/lib/hooks/useMe";
import { useAuctionSocket } from "@/lib/hooks/useAuctionSocket";

import { getAuctionBids } from "@/lib/api/bid";
import { BidResponse } from "@/types/bid";

interface AuctionDetailPageProps {
    params: Promise<{ auctionId: string }>;
}

export default function AuctionDetailPage({ params }: AuctionDetailPageProps) {
    const { auctionId } = use(params);
    const id = Number(auctionId);
    const router = useRouter();

    const { data: auction, isLoading, isError } = useAuction(id);
    const { data: me } = useMe();
    const { mutate: withdrawAuction, isPending } = useDeleteAuction();

    const [bids, setBids] = useState<BidResponse[]>([]);
    const [currentPrice, setCurrentPrice] = useState(0);
    const [activeTab, setActiveTab] = useState<"all" | "mine">("all");

    const isSeller = me?.userId === auction?.sellerId;
    const hasBids = auction?.lastBidderId != null;

    // ✅ normalize (REST + WS 통합)
    const normalizeBid = (raw: any): BidResponse => ({
        auctionId: raw.auctionId,
        bidPrice: raw.bidPrice ?? raw.currentPrice,
        bidderId: raw.bidderId,
        bidedAt:
            raw.bidedAt ??
            new Date().toISOString(),
    });

    // ✅ 초기 데이터
    useEffect(() => {
        if (!auction) return;

        const fetchBids = async () => {
            const res = await getAuctionBids(id);
            const normalized = res.result.map(normalizeBid);

            // 최신순 정렬 (내림차순)
            normalized.sort(
                (a:any, b:any) =>
                    new Date(b.bidedAt).getTime() -
                    new Date(a.bidedAt).getTime()
            );

            setBids(normalized);
            setCurrentPrice(auction.currentPrice);
        };

        fetchBids();
    }, [auction, id]);

    // ✅ WS 실시간 반영
    useAuctionSocket({
        auctionId: id,
        onMessage: (rawBid) => {
            const bid = normalizeBid(rawBid);

            setBids((prev) => {
                // 중복 방지
                const exists = prev.some(
                    (b) =>
                        b.bidderId === bid.bidderId &&
                        b.bidedAt === bid.bidedAt
                );
                if (exists) return prev;

                return [bid, ...prev];
            });

            setCurrentPrice(bid.bidPrice);
        },
    });

    // ✅ 내 입찰 필터링
    const myBids = bids.filter(
        (bid) => bid.bidderId === me?.userId
    );

    const handleWithdraw = () => {
        if (confirm("정말로 이 경매를 철회하시겠습니까?")) {
            withdrawAuction(id);
        }
    };

    return (
        <div className="flex flex-col gap-6 max-w-5xl mx-auto">

            {/* 상단 */}
            <div className="flex flex-col gap-2">
                <BackButton href="/" label="경매 목록" />

                {isSeller && (
                    <div className="flex gap-2">
                        <Button
                            className="bg-yellow-100 text-yellow-700 hover:bg-yellow-200 font-bold"
                            onClick={() => router.push(`/auctions/${id}/edit`)}
                            disabled={hasBids}
                        >
                            경매 수정하기
                        </Button>

                        <Button
                            variant="destructive"
                            onClick={handleWithdraw}
                            disabled={isPending || hasBids}
                        >
                            {isPending ? "철회 중..." : "경매 철회하기"}
                        </Button>
                    </div>
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
                    <AuctionDetailPanel
                        auction={auction}
                        currentPrice={currentPrice}
                    />

                    <BidPanel
                        auction={auction}
                        currentPrice={currentPrice}
                    />

                    {/* ✅ 탭 */}
                    <div className="flex gap-2">
                        <Button
                            variant={activeTab === "all" ? "default" : "outline"}
                            onClick={() => setActiveTab("all")}
                        >
                            전체 입찰
                        </Button>

                        <Button
                            variant={activeTab === "mine" ? "default" : "outline"}
                            onClick={() => setActiveTab("mine")}
                        >
                            내 입찰
                        </Button>
                    </div>

                    {/* ✅ 컨텐츠 */}
                    {activeTab === "all" && (
                        <BidHistoryChart bids={bids} meId={me?.userId} />
                    )}

                    {activeTab === "mine" && (
                        <BidHistoryChart bids={myBids} meId={me?.userId} />
                    )}
                </>
            )}
        </div>
    );
}
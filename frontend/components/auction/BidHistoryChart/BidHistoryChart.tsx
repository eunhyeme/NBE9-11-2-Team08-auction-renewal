"use client";

import { useEffect, useRef } from "react";
import { BidResponse } from "@/types/bid";

interface BidHistoryChartProps {
    bids: BidResponse[];
    meId?: number;
}

export function BidHistoryChart({ bids, meId }: BidHistoryChartProps) {
    const containerRef = useRef<HTMLDivElement>(null);

    // 자동 스크롤 (최하단 유지)
    useEffect(() => {
        const el = containerRef.current;
        if (!el) return;

        const isAtBottom =
            el.scrollTop + el.clientHeight >= el.scrollHeight - 10;

        if (isAtBottom) {
            el.scrollTo({
                top: el.scrollHeight,
                behavior: "smooth",
            });
        }
    }, [bids]);

    return (
        <div className="rounded-xl border p-4 bg-card">
            <h3 className="font-semibold mb-2">입찰 히스토리</h3>

            <div
                ref={containerRef}
                className="h-64 overflow-y-auto flex flex-col gap-2"
            >
                {bids.length === 0 ? (
                    <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                        입찰 내역이 없습니다
                    </div>
                ) : (
                    bids.map((bid, index) => {
                        const isMine = bid.bidderId === meId;

                        return (
                            <div
                                key={`${bid.bidderId}-${bid.bidedAt}-${index}`}
                                className={`flex justify-between text-sm border-b px-2 py-1 rounded ${
                                    isMine ? "bg-yellow-100" : ""
                                }`}
                            >
                                {/* 왼쪽: 사용자 + 시간 */}
                                <div className="flex flex-col">
            <span className="text-xs text-gray-500">
              {new Date(bid.bidedAt).toLocaleTimeString()}
            </span>

                                    <span className="font-medium">
              {bid.nickname}
                                        {isMine && " (나)"}
            </span>
                                </div>

                                {/* 오른쪽: 가격 */}
                                <span
                                    className={`font-semibold ${
                                        isMine
                                            ? "text-yellow-600"
                                            : "text-[var(--color-brand-600)]"
                                    }`}
                                >
            {bid.bidPrice.toLocaleString()}원
          </span>
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
}
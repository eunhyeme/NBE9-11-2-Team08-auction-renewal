"use client";

import { useState, useEffect } from "react";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { AuctionCard, AuctionCardSkeleton } from "@/components/auction/AuctionCard";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { useAuctions } from "@/lib/hooks/useAuctions";

const PAGE_SIZE = 12;

interface AuctionGridProps {
  keyword?: string;
  sort?: string;
  minPrice?: number;
  maxPrice?: number;
  bluecheckOnly?: boolean;
}

export function AuctionGrid({ keyword, sort, minPrice, maxPrice, bluecheckOnly }: AuctionGridProps = {}) {
  const [page, setPage] = useState(0);

  useEffect(() => {
    setPage(0);
  }, [keyword, sort, minPrice, maxPrice, bluecheckOnly]);

  const { data, isLoading, isError } = useAuctions(page, PAGE_SIZE, keyword, sort, minPrice, maxPrice, bluecheckOnly);

  if (isError) return <ErrorMessage message="경매 목록을 불러오는 데 실패했습니다." />;

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <AuctionCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!data?.content.length) {
    return <EmptyState message="진행 중인 경매가 없습니다." />;
  }

  return (
    <div className="flex flex-col gap-6">
      {/* 그리드 */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
        {data.content.map((auction) => (
          <AuctionCard key={auction.auctionId} auction={auction} />
        ))}
      </div>

      {/* 페이지네이션 */}
      {data.totalPages > 1 && (
        <Pagination>
          <PaginationContent>
            <PaginationItem>
              <PaginationPrevious
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                aria-disabled={data.first}
                className={data.first ? "pointer-events-none opacity-50" : "cursor-pointer"}
              />
            </PaginationItem>

            {Array.from({ length: data.totalPages }).map((_, i) => (
              <PaginationItem key={i}>
                <PaginationLink
                  isActive={i === page}
                  onClick={() => setPage(i)}
                  className="cursor-pointer"
                >
                  {i + 1}
                </PaginationLink>
              </PaginationItem>
            ))}

            <PaginationItem>
              <PaginationNext
                onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                aria-disabled={data.last}
                className={data.last ? "pointer-events-none opacity-50" : "cursor-pointer"}
              />
            </PaginationItem>
          </PaginationContent>
        </Pagination>
      )}
    </div>
  );
}

import Image from "next/image";
import Link from "next/link";
import { AuctionStatusBadge } from "@/components/auction/AuctionStatusBadge";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { CountdownTimer } from "@/components/common/CountdownTimer";
import { Skeleton } from "@/components/ui/skeleton";
import type { AuctionResponse } from "@/types/auction";

interface AuctionCardProps {
  auction: AuctionResponse;
}

export function AuctionCard({ auction }: AuctionCardProps) {
  return (
    <Link href={`/auctions/${auction.auctionId}`}>
      <div className="group flex flex-col overflow-hidden rounded-xl border bg-card transition-shadow hover:shadow-md">
        {/* 이미지 */}
        <div className="relative aspect-square w-full overflow-hidden bg-muted">
          {auction.imageUrl ? (
            <Image
              src={auction.imageUrl}
              alt={auction.title}
              fill
              className="object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-muted-foreground text-xs">
              No Image
            </div>
          )}
          {/* 상태 배지 (이미지 위) */}
          <div className="absolute top-2 left-2">
            <AuctionStatusBadge status={auction.status} />
          </div>
        </div>

        {/* 내용 */}
        <div className="flex flex-col gap-2 p-3">
          <p className="font-medium line-clamp-2 text-sm leading-snug">
            {auction.title}
          </p>
          <PriceDisplay amount={auction.currentPrice} label="현재가" />
          {auction.status === "ONGOING" && (
            <CountdownTimer endedAt={auction.endedAt} className="text-xs" />
          )}
        </div>
      </div>
    </Link>
  );
}

export function AuctionCardSkeleton() {
  return (
    <div className="flex flex-col overflow-hidden rounded-xl border bg-card">
      <Skeleton className="aspect-square w-full" />
      <div className="flex flex-col gap-2 p-3">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-5 w-1/2" />
        <Skeleton className="h-3 w-1/3" />
      </div>
    </div>
  );
}

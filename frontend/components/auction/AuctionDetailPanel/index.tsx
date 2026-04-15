import Image from "next/image";
import { AuctionStatusBadge } from "@/components/auction/AuctionStatusBadge";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { CountdownTimer } from "@/components/common/CountdownTimer";
import { Separator } from "@/components/ui/separator";
import { formatDate } from "@/lib/utils/formatDate";
import { formatPrice } from "@/lib/utils/formatPrice";
import type { AuctionResponse } from "@/types/auction";

interface AuctionDetailPanelProps {
  auction: AuctionResponse;
}

export function AuctionDetailPanel({ auction }: AuctionDetailPanelProps) {
  return (
    <div className="flex flex-col gap-6 md:flex-row">
      {/* 이미지 */}
      <div className="relative aspect-square w-full max-w-sm shrink-0 overflow-hidden rounded-xl bg-muted md:w-80">
        {auction.imageUrl ? (
          <Image
            src={auction.imageUrl}
            alt={auction.title}
            fill
            className="object-cover"
            priority
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-muted-foreground">
            No Image
          </div>
        )}
      </div>

      {/* 정보 */}
      <div className="flex flex-1 flex-col gap-4">
        {/* 상태 + 제목 */}
        <div className="flex flex-col gap-2">
          <AuctionStatusBadge status={auction.status} />
          <h1 className="text-2xl font-bold leading-tight">{auction.title}</h1>
        </div>

        <Separator />

        {/* 가격 정보 */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-xs text-muted-foreground mb-1">시작가</p>
            <p className="font-semibold">{formatPrice(auction.startPrice)}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground mb-1">입찰 단위</p>
            <p className="font-semibold">{formatPrice(auction.bidUnit)}</p>
          </div>
        </div>

        <div>
          <PriceDisplay amount={auction.currentPrice} label="현재가" size="lg" />
        </div>

        {/* 남은 시간 */}
        {auction.status === "ONGOING" && (
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">마감:</span>
            <CountdownTimer endedAt={auction.endedAt} />
            <span className="text-xs text-muted-foreground">
              ({formatDate(auction.endedAt)})
            </span>
          </div>
        )}

        {/* 설명 */}
        {auction.description && (
          <>
            <Separator />
            <div>
              <p className="text-sm text-muted-foreground mb-1">상품 설명</p>
              <p className="text-sm whitespace-pre-wrap leading-relaxed">
                {auction.description}
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

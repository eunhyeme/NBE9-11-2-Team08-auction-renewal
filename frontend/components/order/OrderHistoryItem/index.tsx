import Image from "next/image";
import Link from "next/link";
import { OrderStatusBadge } from "@/components/order/OrderStatusBadge";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { formatDate } from "@/lib/utils/formatDate";
import { resolveImageUrl } from "@/lib/utils/resolveImageUrl";
import type { OrderResponse } from "@/types/order";
import type { AuctionResponse } from "@/types/auction";

interface OrderHistoryItemProps {
  order: OrderResponse;
  auction?: Pick<AuctionResponse, "title" | "imageUrl">;
}

export function OrderHistoryItem({ order, auction }: OrderHistoryItemProps) {
  const title = auction?.title ?? order.auctionTitle ?? `주문 #${order.orderId}`;
  const imageUrl = resolveImageUrl(auction?.imageUrl ?? order.auctionImageUrl);

  return (
    <Link href={`/orders/${order.orderId}`}>
      <div className="flex items-center gap-4 rounded-lg border bg-card p-4 hover:bg-muted/50 transition-colors">
        {/* 이미지 */}
        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-md bg-muted">
          {imageUrl ? (
            <Image
              src={imageUrl}
              alt={title}
              fill
              className="object-cover"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-muted-foreground text-xs">
              No Image
            </div>
          )}
        </div>

        {/* 정보 */}
        <div className="flex flex-1 flex-col gap-1 min-w-0">
          <p className="font-medium truncate">
            {title}
          </p>
          <div className="flex items-center gap-2">
            <OrderStatusBadge status={order.status} />
            <span className="text-xs text-muted-foreground">
              주문 #{order.orderId}
            </span>
            <span className="text-xs text-muted-foreground">
              {formatDate(order.createdAt)}
            </span>
          </div>
        </div>

        {/* 금액 */}
        <PriceDisplay amount={order.finalPrice} />
      </div>
    </Link>
  );
}

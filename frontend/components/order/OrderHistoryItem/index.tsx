import Image from "next/image";
import Link from "next/link";
import { OrderStatusBadge } from "@/components/order/OrderStatusBadge";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { formatDate } from "@/lib/utils/formatDate";
import type { OrderResponse } from "@/types/order";
import type { AuctionResponse } from "@/types/auction";

interface OrderHistoryItemProps {
  order: OrderResponse;
  auction?: Pick<AuctionResponse, "title" | "imageUrl">;
}

export function OrderHistoryItem({ order, auction }: OrderHistoryItemProps) {
  return (
    <Link href={`/orders/${order.orderId}`}>
      <div className="flex items-center gap-4 rounded-lg border bg-card p-4 hover:bg-muted/50 transition-colors">
        {/* 이미지 */}
        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-md bg-muted">
          {auction?.imageUrl ? (
            <Image
              src={auction.imageUrl}
              alt={auction.title ?? "경매 상품"}
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
            {auction?.title ?? `주문 #${order.orderId}`}
          </p>
          <div className="flex items-center gap-2">
            <OrderStatusBadge status={order.status} />
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

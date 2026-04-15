import { PriceDisplay } from "@/components/common/PriceDisplay";
import { OrderStatusBadge } from "@/components/order/OrderStatusBadge";
import { CountdownTimer } from "@/components/common/CountdownTimer";
import { Separator } from "@/components/ui/separator";
import { formatDate } from "@/lib/utils/formatDate";
import type { OrderResponse } from "@/types/order";

interface OrderDetailCardProps {
  order: OrderResponse;
  auctionTitle?: string;
}

export function OrderDetailCard({ order, auctionTitle }: OrderDetailCardProps) {
  return (
    <div className="rounded-xl border bg-card p-5 flex flex-col gap-4">
      {/* 헤더: 상태 + 주문번호 */}
      <div className="flex items-center justify-between">
        <OrderStatusBadge status={order.status} />
        <span className="text-xs text-muted-foreground">
          주문번호 #{order.orderId}
        </span>
      </div>

      {auctionTitle && (
        <p className="font-medium text-sm line-clamp-2">{auctionTitle}</p>
      )}

      <Separator />

      {/* 가격 */}
      <PriceDisplay amount={order.finalPrice} label="낙찰가" size="lg" />

      {/* 날짜 정보 */}
      <div className="flex flex-col gap-1.5 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">주문일</span>
          <span>{formatDate(order.createdAt)}</span>
        </div>

        {order.status === "AWAITING_PAYMENT" && (
          <div className="flex justify-between items-center">
            <span className="text-muted-foreground">결제 기한</span>
            <div className="flex items-center gap-1.5">
              <CountdownTimer endedAt={order.paymentDeadline} />
              <span className="text-xs text-muted-foreground">
                ({formatDate(order.paymentDeadline)})
              </span>
            </div>
          </div>
        )}

        {order.paidAt && (
          <div className="flex justify-between">
            <span className="text-muted-foreground">결제일</span>
            <span>{formatDate(order.paidAt)}</span>
          </div>
        )}
      </div>

      {/* 배송지 */}
      {order.shippingAddress && (
        <>
          <Separator />
          <div className="flex flex-col gap-1">
            <p className="text-xs text-muted-foreground">배송지</p>
            <p className="text-sm">{order.shippingAddress}</p>
          </div>
        </>
      )}

      {/* 송장번호 */}
      {order.trackingNumber && (
        <div className="flex flex-col gap-1">
          <p className="text-xs text-muted-foreground">송장번호</p>
          <p className="text-sm font-mono">{order.trackingNumber}</p>
        </div>
      )}
    </div>
  );
}

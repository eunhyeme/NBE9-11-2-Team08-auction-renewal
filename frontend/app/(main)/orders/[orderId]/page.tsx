"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import Link from "next/link";

import { OrderDetailCard } from "@/components/order/OrderDetailCard";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Button, buttonVariants } from "@/components/ui/button";
import { useOrder, usePayOrder } from "@/lib/hooks/useOrder";
import { useAuction } from "@/lib/hooks/useAuction";
import { ApiError } from "@/types/api";
import { cn } from "@/lib/utils";

interface OrderDetailPageProps {
  params: Promise<{ orderId: string }>;
}

export default function OrderDetailPage({ params }: OrderDetailPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const { data: order, isLoading, isError } = useOrder(id);
  const { data: auction } = useAuction(order?.auctionId ?? 0);
  const payOrder = usePayOrder(id);

  const handlePay = async () => {
    try {
      await payOrder.mutateAsync();
      toast.success("결제가 완료되었습니다.");
      router.push(`/orders/${id}/success`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("결제 중 오류가 발생했습니다.");
      }
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <LoadingSpinner />
      </div>
    );
  }

  if (isError || !order) {
    return <ErrorMessage message="주문 정보를 불러오는 데 실패했습니다." />;
  }

  return (
    <div className="max-w-lg mx-auto flex flex-col gap-6">
      <h1 className="text-2xl font-bold">주문 상세</h1>

      <OrderDetailCard order={order} auctionTitle={auction?.title} />

      {/* 배송지 변경 링크 */}
      <div className="flex items-center justify-between rounded-xl border bg-card p-4">
        <div className="flex flex-col gap-0.5">
          <p className="text-xs text-muted-foreground">배송지</p>
          <p className="text-sm">
            {order.shippingAddress ?? "배송지가 등록되지 않았습니다."}
          </p>
        </div>
        <Link
          href={`/orders/${id}/address`}
          className={cn(buttonVariants({ variant: "outline", size: "sm" }))}
        >
          변경
        </Link>
      </div>

      {/* 결제하기 버튼 (AWAITING_PAYMENT 상태에서만) */}
      {order.status === "AWAITING_PAYMENT" && (
        <Button
          size="lg"
          onClick={handlePay}
          disabled={payOrder.isPending || !order.shippingAddress}
        >
          {payOrder.isPending ? "결제 중..." : "결제하기"}
        </Button>
      )}

      {!order.shippingAddress && order.status === "AWAITING_PAYMENT" && (
        <p className="text-xs text-destructive text-center">
          배송지를 먼저 등록해 주세요.
        </p>
      )}
    </div>
  );
}

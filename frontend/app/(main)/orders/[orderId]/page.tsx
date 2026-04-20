"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import Link from "next/link";
import { Truck, MapPin } from "lucide-react"; 

import { OrderDetailCard } from "@/components/order/OrderDetailCard";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Button, buttonVariants } from "@/components/ui/button";
import { useOrder, usePayOrder } from "@/lib/hooks/useOrder";
import { useAuction } from "@/lib/hooks/useAuction";
import { useMe } from "@/lib/hooks/useMe";
import { ApiError } from "@/types/api";
import { cn } from "@/lib/utils";

interface OrderDetailPageProps {
  params: Promise<{ orderId: string }>;
}

export default function OrderDetailPage({ params }: OrderDetailPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const { data: me, isLoading: isMeLoading } = useMe();
  const { data: order, isLoading: isOrderLoading, isError } = useOrder(id);
  const { data: auction } = useAuction(order?.auctionId ?? 0);
  const payOrder = usePayOrder(id);

  if (isOrderLoading || isMeLoading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>;
  if (isError || !order || !me) return <ErrorMessage message="정보를 불러오는 데 실패했습니다." />;

  const isBuyer = Number(me.userId) === Number(order.bidderId);
  const isSeller = Number(me.userId) === Number(order.sellerId);

  const handlePay = async () => {
    try {
      await payOrder.mutateAsync();
      toast.success("결제가 완료되었습니다.");
      router.refresh();
    } catch (err) {
      if (err instanceof ApiError) toast.error(err.message);
      else toast.error("결제 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="max-w-lg mx-auto flex flex-col gap-6">
      <h1 className="text-2xl font-bold">주문 상세</h1>

      {/* 날짜 정보가 포함된 메인 카드 */}
      <OrderDetailCard order={order} auctionTitle={auction?.title} />

      {/* 운송장 번호 박스 (있을 때만) */}
      {order.trackingNumber && (
        <div className="flex items-center gap-3 rounded-xl border bg-blue-50/50 p-4 border-blue-100">
          <Truck className="h-5 w-5 text-blue-600" />
          <div className="flex flex-col">
            <p className="text-xs text-blue-600 font-semibold">배송 정보</p>
            <p className="text-sm font-bold text-blue-900">운송장 번호: {order.trackingNumber}</p>
          </div>
        </div>
      )}

      {/* 하단 버튼 영역 */}
      <div className="flex flex-col gap-4">
        {isBuyer && !order.trackingNumber && (
          <Link
            href={`/orders/${id}/address`}
            className={cn(buttonVariants({ variant: "outline", size: "lg" }), "w-full gap-2 border-dashed border-2")}
          >
            <MapPin className="h-4 w-4" /> 배송지 변경하기
          </Link>
        )}

        {isBuyer && order.status === "AWAITING_PAYMENT" && (
          <Button size="lg" onClick={handlePay} disabled={payOrder.isPending || !order.shippingAddress} className="h-14 text-lg">
            {payOrder.isPending ? "결제 중..." : "결제하기"}
          </Button>
        )}

        {isSeller && order.status === "PAID" && !order.trackingNumber && (
          <Link href={`/orders/${id}/tracking`} className={cn(buttonVariants({ size: "lg" }), "h-14 text-lg")}>
            운송장 번호 등록하기
          </Link>
        )}
      </div>
    </div>
  );
}
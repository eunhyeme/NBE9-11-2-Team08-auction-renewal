"use client";

import { use } from "react";
import { PaymentSuccessCard } from "@/components/order/PaymentSuccessCard";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { useOrder } from "@/lib/hooks/useOrder";
import { useAuction } from "@/lib/hooks/useAuction";

interface OrderSuccessPageProps {
  params: Promise<{ orderId: string }>;
}

export default function OrderSuccessPage({ params }: OrderSuccessPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);

  const { data: order, isLoading, isError } = useOrder(id);
  const { data: auction } = useAuction(order?.auctionId ?? 0);

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
    <div className="max-w-lg mx-auto py-8">
      <PaymentSuccessCard
        orderId={order.orderId}
        finalPrice={order.finalPrice}
        auctionTitle={auction?.title}
      />
    </div>
  );
}

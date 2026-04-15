"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { ShippingAddressForm } from "@/components/order/ShippingAddressForm";
import { BackButton } from "@/components/common/BackButton";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { useOrder, useUpdateShipping } from "@/lib/hooks/useOrder";
import { ApiError } from "@/types/api";

interface AddressPageProps {
  params: Promise<{ orderId: string }>;
}

export default function AddressPage({ params }: AddressPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const { data: order, isLoading, isError } = useOrder(id);
  const updateShipping = useUpdateShipping(id);

  const handleSubmit = async (address: string) => {
    try {
      await updateShipping.mutateAsync(address);
      toast.success("배송지가 저장되었습니다.");
      router.push(`/orders/${id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("배송지 저장 중 오류가 발생했습니다.");
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

  if (isError) {
    return <ErrorMessage message="주문 정보를 불러오는 데 실패했습니다." />;
  }

  return (
    <div className="max-w-lg mx-auto flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <BackButton href={`/orders/${id}`} label="주문 상세" />
        <h1 className="text-2xl font-bold">배송지 변경</h1>
      </div>

      <ShippingAddressForm
        defaultAddress={order?.shippingAddress ?? ""}
        onSubmit={handleSubmit}
        submitLabel="저장"
      />
    </div>
  );
}

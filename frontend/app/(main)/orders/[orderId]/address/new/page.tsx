"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { ShippingAddressForm } from "@/components/order/ShippingAddressForm";
import { BackButton } from "@/components/common/BackButton";
import { useUpdateShipping } from "@/lib/hooks/useOrder";
import { ApiError } from "@/types/api";

interface AddressNewPageProps {
  params: Promise<{ orderId: string }>;
}

export default function AddressNewPage({ params }: AddressNewPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const updateShipping = useUpdateShipping(id);

  const handleSubmit = async (address: string) => {
    try {
      await updateShipping.mutateAsync(address);
      toast.success("배송지가 등록되었습니다.");
      router.push(`/orders/${id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("배송지 등록 중 오류가 발생했습니다.");
      }
    }
  };

  return (
    <div className="max-w-lg mx-auto flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <BackButton href={`/orders/${id}/address`} label="배송지 변경" />
        <h1 className="text-2xl font-bold">새 배송지 추가</h1>
      </div>

      <ShippingAddressForm
        onSubmit={handleSubmit}
        submitLabel="등록"
      />
    </div>
  );
}

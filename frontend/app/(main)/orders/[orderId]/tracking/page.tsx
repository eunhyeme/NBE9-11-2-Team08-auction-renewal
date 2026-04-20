"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { TrackingNumberForm } from "@/components/order/TrackingNumberForm";
import { BackButton } from "@/components/common/BackButton";
import { useRegisterTracking } from "@/lib/hooks/useOrder";
import { ApiError } from "@/types/api";

interface TrackingPageProps {
  params: Promise<{ orderId: string }>;
}

export default function TrackingPage({ params }: TrackingPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const registerTracking = useRegisterTracking(id);

  const handleSubmit = async (carrier: string, trackingNumber: string) => {
    try {
      await registerTracking.mutateAsync(`${carrier} ${trackingNumber}`);
      toast.success("송장번호가 등록되었습니다.");
      router.push(`/orders/${id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("송장번호 등록 중 오류가 발생했습니다.");
      }
    }
  };

  return (
    <div className="max-w-lg mx-auto flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        {/* 뒤로 가기 경로를 주문 상세 페이지로 변경 */}
        <BackButton href={`/orders/${id}`} label="주문 상세" />
        <h1 className="text-2xl font-bold">송장번호 등록</h1>
        <p className="text-sm text-muted-foreground">
          택배사와 송장번호를 입력해 주세요.
        </p>
      </div>

      <TrackingNumberForm onSubmit={handleSubmit} />
    </div>
  );
}
"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import Link from "next/link";
import { MapPin, Plus, Check, ChevronRight, Loader2 } from "lucide-react";

import { BackButton } from "@/components/common/BackButton";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { useOrder, useUpdateShipping } from "@/lib/hooks/useOrder";
import { useMe } from "@/lib/hooks/useMe";
import { ApiError } from "@/types/api";
import { cn } from "@/lib/utils";

interface AddressPageProps {
  params: Promise<{ orderId: string }>;
}

export default function AddressPage({ params }: AddressPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const { data: me, isLoading: isMeLoading } = useMe();
  const { data: order, isLoading: isOrderLoading, isError } = useOrder(id);
  const updateShipping = useUpdateShipping(id);

  const handleSelectDefaultAddress = async () => {
    if (!me?.address) return;
    try {
      await updateShipping.mutateAsync({
        address: me.address,
        saveAsDefault: false
      });
      toast.success("기본 배송지로 설정되었습니다.");
      router.push(`/orders/${id}`);
    } catch (err) {
      if (err instanceof ApiError) toast.error(err.message);
      else toast.error("오류가 발생했습니다.");
    }
  };

  if (isMeLoading || isOrderLoading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>;
  if (isError || !order || !me) return <ErrorMessage message="정보를 불러오는 데 실패했습니다." />;

  return (
    <div className="max-w-lg mx-auto flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <BackButton href={`/orders/${id}`} label="주문 상세" />
        <h1 className="text-2xl font-bold">배송지 선택</h1>
      </div>

      <div className="flex flex-col gap-4">
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-semibold text-muted-foreground ml-1">기본 배송지</h2>
          {me.address ? (
            <button
              onClick={handleSelectDefaultAddress}
              disabled={updateShipping.isPending}
              className={cn(
                "group relative flex flex-col gap-3 rounded-2xl border-2 p-5 text-left transition-all",
                order.shippingAddress === me.address ? "border-primary bg-primary/5" : "border-border bg-card hover:border-primary/50"
              )}
            >
              <div className="flex items-center justify-between">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <MapPin className="h-4 w-4" />
                </div>
                {order.shippingAddress === me.address && (
                  <span className="flex items-center gap-1 text-xs font-bold text-primary"><Check className="h-3 w-3" /> 선택됨</span>
                )}
              </div>
              <div className="flex flex-col gap-1">
                <p className="font-semibold">{me.nickname}님의 기본 주소</p>
                <p className="text-sm text-muted-foreground">{me.address}</p>
              </div>
              {updateShipping.isPending && (
                <div className="absolute inset-0 flex items-center justify-center rounded-2xl bg-white/50">
                  <Loader2 className="h-6 w-6 animate-spin text-primary" />
                </div>
              )}
            </button>
          ) : (
            <div className="rounded-2xl border border-dashed p-8 text-center text-sm text-muted-foreground">등록된 기본 배송지가 없습니다.</div>
          )}
        </section>

        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-semibold text-muted-foreground ml-1">기타</h2>
          <Link
            href={`/orders/${id}/address/new`}
            className="group flex items-center justify-between rounded-2xl border-2 border-border bg-card p-5 transition-all hover:border-primary/50 hover:bg-accent/50"
          >
            <div className="flex items-center gap-4">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-muted text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary">
                <Plus className="h-4 w-4" />
              </div>
              <div className="flex flex-col gap-1">
                <p className="font-semibold">새 배송지 직접 입력</p>
                <p className="text-sm text-muted-foreground">주소 검색을 통해 새로 등록합니다.</p>
              </div>
            </div>
            <ChevronRight className="h-5 w-5 text-muted-foreground" />
          </Link>
        </section>
      </div>
    </div>
  );
}
"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { BackButton } from "@/components/common/BackButton";
import { AddressInput } from "@/components/common/AddressInput";
import { useUpdateShipping } from "@/lib/hooks/useOrder";
import { 
  shippingSchema, 
  type ShippingFormValues 
} from "@/lib/validations/orderSchema";
import { ApiError } from "@/types/api";

interface AddressNewPageProps {
  params: Promise<{ orderId: string }>;
}

export default function AddressNewPage({ params }: AddressNewPageProps) {
  const { orderId } = use(params);
  const id = Number(orderId);
  const router = useRouter();

  const updateShipping = useUpdateShipping(id);

  const {
    control,
    register, // 체크박스 등록을 위해 추가
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ShippingFormValues>({
    resolver: zodResolver(shippingSchema),
    defaultValues: { 
      shippingAddress: "",
      saveAsDefault: false // 기본값 설정
    },
  });

  const onSubmit = async (values: ShippingFormValues) => {
    try {
      // 훅이 객체 형태 { address, saveAsDefault }를 받도록 수정되었으므로 맞춰서 전달
      await updateShipping.mutateAsync({
        address: values.shippingAddress,
        saveAsDefault: values.saveAsDefault
      });
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

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        {/* 주소 검색 입력란 */}
        <div className="flex flex-col gap-1.5">
          <Label>배송지 주소</Label>
          <Controller
            control={control}
            name="shippingAddress"
            render={({ field }) => (
              <AddressInput
                value={field.value}
                onChange={field.onChange}
                error={errors.shippingAddress?.message}
              />
            )}
          />
        </div>

        {/* 기본 배송지 저장 체크박스 추가 */}
        <div className="flex items-center gap-2 px-1">
          <input
            type="checkbox"
            id="saveAsDefault"
            className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
            {...register("saveAsDefault")}
          />
          <Label htmlFor="saveAsDefault" className="text-sm font-normal cursor-pointer">
            이 주소를 기본 배송지로 저장
          </Label>
        </div>

        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          등록
        </Button>
      </form>
    </div>
  );
}
"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import {
  shippingSchema,
  type ShippingFormValues,
} from "@/lib/validations/orderSchema";

interface ShippingAddressFormProps {
  defaultAddress?: string;
  // string 대신 객체 전체(ShippingFormValues)를 받도록 수정
  onSubmit: (values: ShippingFormValues) => Promise<void>;
  submitLabel?: string;
}

export function ShippingAddressForm({
  defaultAddress = "",
  onSubmit,
  submitLabel = "저장",
}: ShippingAddressFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ShippingFormValues>({
    resolver: zodResolver(shippingSchema),
    defaultValues: { 
      shippingAddress: defaultAddress,
      saveAsDefault: false 
    },
  });

  return (
    <form
      onSubmit={handleSubmit((values) => onSubmit(values))}
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="shippingAddress">배송지 주소</Label>
        <Input
          id="shippingAddress"
          placeholder="배송 받을 주소를 입력해 주세요"
          {...register("shippingAddress")}
        />
        {errors.shippingAddress && (
          <p className="text-xs text-destructive">
            {errors.shippingAddress.message}
          </p>
        )}
      </div>

      {/* 기본 배송지 설정 체크박스 */}
      <div className="flex items-center gap-2 px-1">
        <input
          type="checkbox"
          id="saveAsDefault"
          className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
          {...register("saveAsDefault")}
        />
        <Label htmlFor="saveAsDefault" className="text-sm font-normal cursor-pointer">
          기본 배송지로 설정
        </Label>
      </div>

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        {submitLabel}
      </Button>
    </form>
  );
}
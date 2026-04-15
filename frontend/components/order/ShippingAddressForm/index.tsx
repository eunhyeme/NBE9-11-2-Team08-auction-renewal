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
  onSubmit: (address: string) => Promise<void>;
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
    defaultValues: { shippingAddress: defaultAddress },
  });

  return (
    <form
      onSubmit={handleSubmit((v) => onSubmit(v.shippingAddress))}
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

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        {submitLabel}
      </Button>
    </form>
  );
}

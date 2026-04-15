"use client";

import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import {
  trackingSchema,
  type TrackingFormValues,
} from "@/lib/validations/orderSchema";

const CARRIERS = [
  { value: "CJ대한통운", label: "CJ대한통운" },
  { value: "롯데택배", label: "롯데택배" },
  { value: "한진택배", label: "한진택배" },
  { value: "우체국택배", label: "우체국택배" },
  { value: "로젠택배", label: "로젠택배" },
  { value: "경동택배", label: "경동택배" },
];

interface TrackingNumberFormProps {
  onSubmit: (carrier: string, trackingNumber: string) => Promise<void>;
}

export function TrackingNumberForm({ onSubmit }: TrackingNumberFormProps) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<TrackingFormValues>({
    resolver: zodResolver(trackingSchema),
  });

  return (
    <form
      onSubmit={handleSubmit((v) => onSubmit(v.carrier, v.trackingNumber))}
      className="flex flex-col gap-4"
    >
      {/* 택배사 선택 */}
      <div className="flex flex-col gap-1.5">
        <Label>택배사</Label>
        <Controller
          control={control}
          name="carrier"
          render={({ field }) => (
            <Select value={field.value} onValueChange={field.onChange}>
              <SelectTrigger>
                <SelectValue placeholder="택배사를 선택해 주세요" />
              </SelectTrigger>
              <SelectContent>
                {CARRIERS.map((c) => (
                  <SelectItem key={c.value} value={c.value}>
                    {c.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
        {errors.carrier && (
          <p className="text-xs text-destructive">{errors.carrier.message}</p>
        )}
      </div>

      {/* 송장번호 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="trackingNumber">송장번호</Label>
        <Input
          id="trackingNumber"
          type="text"
          inputMode="numeric"
          placeholder="숫자만 입력해 주세요"
          {...register("trackingNumber")}
        />
        {errors.trackingNumber && (
          <p className="text-xs text-destructive">
            {errors.trackingNumber.message}
          </p>
        )}
      </div>

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        등록하기
      </Button>
    </form>
  );
}

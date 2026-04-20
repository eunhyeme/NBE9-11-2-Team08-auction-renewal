"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useUpdateAuction } from "@/lib/hooks/useAuction";
import { auctionSchema, type AuctionFormValues } from "@/lib/validations/auctionSchema";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { AuctionResponse } from "@/types/auction";
import { resolveImageUrl } from "@/lib/utils/resolveImageUrl";

interface AuctionEditFormProps {
  auction: AuctionResponse;
}

export default function AuctionEditForm({ auction }: AuctionEditFormProps) {
  const { mutate: mutateUpdate, isPending } = useUpdateAuction();
  const [preview, setPreview] = useState<string | null>(resolveImageUrl(auction.imageUrl));

  // 경매 기간(일수) 계산
  const calculateDuration = () => {
    const started = new Date(auction.startedAt);
    const ended = new Date(auction.endedAt);
    return Math.ceil(Math.abs(ended.getTime() - started.getTime()) / (1000 * 60 * 60 * 24));
  };

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<AuctionFormValues>({
    resolver: zodResolver(auctionSchema) as any,
    defaultValues: {
      title: auction.title,
      description: auction.description ?? "",
      startPrice: auction.startPrice,
      bidUnit: auction.bidUnit,
      duration: calculateDuration(),
    },
  });

  const onSubmit = (data: AuctionFormValues) => {
    mutateUpdate({ auctionId: auction.auctionId, data });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6 max-w-xl mx-auto" noValidate>
      {/* 이미지 섹션 */}
      <div className="flex flex-col gap-2">
        <Label>상품 이미지 (변경 시에만 업로드)</Label>
        <div 
        className="w-full h-48 border-2 border-dashed border-[var(--color-brand-500)] rounded-lg flex items-center justify-center overflow-hidden cursor-pointer bg-white" // 💡 bg-white 추가
        onClick={() => document.getElementById("image-upload")?.click()}
        >
        {preview ? (
            <img 
            src={preview} 
            alt="미리보기" 
            className="w-full h-full object-contain" // 💡 object-contain 적용
            />
        ) : (
            <span className="text-[var(--color-brand-600)]">클릭하여 이미지 업로드</span>
        )}
        </div>
        <input
          id="image-upload"
          type="file"
          className="hidden"
          accept="image/*"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) {
              setValue("image", file);
              setPreview(URL.createObjectURL(file));
            }
          }}
        />
        <p className="text-xs text-muted-foreground">이미지를 선택하지 않으면 기존 이미지가 유지됩니다.</p>
      </div>

      {/* 제목 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="title">경매 제목</Label>
        <Input id="title" {...register("title")} placeholder="경매의 제목을 입력하세요" />
        {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
      </div>

      {/* 가격 설정 */}
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1.5">
          <Label>시작가 (<PriceDisplay amount={watch("startPrice") || 0} />)</Label>
          <Input 
            type="number" 
            {...register("startPrice", { valueAsNumber: true })} 
            className="[appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <Label>입찰 단위 (<PriceDisplay amount={watch("bidUnit") || 0} />)</Label>
          <Input 
            type="number" 
            step="100" 
            {...register("bidUnit", { valueAsNumber: true })} 
          />
          {errors.bidUnit && <p className="text-xs text-destructive">{errors.bidUnit.message as string}</p>}
        </div>
      </div>

      {/* 경매 기간 */}
      <div className="flex flex-col gap-1.5">
        <Label>경매 기간 (1~10일)</Label>
        <Input type="number" min="1" max="10" {...register("duration", { valueAsNumber: true })} />
        {errors.duration && <p className="text-xs text-destructive">{errors.duration.message}</p>}
      </div>

      {/* 상세 설명 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">상세 설명</Label>
        <Textarea id="description" {...register("description")} className="h-32" />
        {errors.description && <p className="text-xs text-destructive">{errors.description.message}</p>}
      </div>

      <Button 
        type="submit" 
        disabled={isPending}
        className="bg-[var(--color-brand-500)] hover:bg-[var(--color-brand-600)] text-white h-12 text-lg font-bold"
      >
        {isPending ? "수정 중..." : "경매 정보 수정하기"}
      </Button>
    </form>
  );
}

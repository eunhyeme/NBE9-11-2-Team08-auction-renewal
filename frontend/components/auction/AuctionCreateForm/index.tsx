"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useCreateAuction } from "@/lib/hooks/useAuction";
import { auctionSchema, type AuctionFormValues } from "@/lib/validations/auctionSchema";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner"; // 💡 toast 임포트 추가

export function AuctionCreateForm() {
  const router = useRouter();
  const { mutateAsync: server_createAuction, isPending } = useCreateAuction();
  const [preview, setPreview] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<AuctionFormValues>({
    resolver: zodResolver(auctionSchema) as any,
    // startPrice와 bidUnit을 초기값에서 제외하거나 undefined로 설정하여 placeholder가 보이게 합니다
    defaultValues: { 
      title: "",
      description: "",
      duration: 1 
    },
  });

  const onSubmit = async (data: AuctionFormValues) => {
    try {
      const result = await server_createAuction(data);
      // 💡 alert 대신 toast.success 사용!
      toast.success("경매가 성공적으로 등록되었습니다!");
      router.push(`/auctions/${result.auctionId}`);
    } catch (err) {
      // 💡 에러 메시지도 toast.error로 깔끔하게 전달
      const errorMessage = err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.";
      toast.error(`경매 등록 실패: ${errorMessage}`);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6 max-w-xl mx-auto"noValidate>
      {/* 이미지 미리보기 구역 */}
      <div className="flex flex-col gap-2">
        <Label>상품 이미지</Label>
        <div 
          className="w-full h-48 border-2 border-dashed border-[var(--color-brand-500)] rounded-lg flex items-center justify-center overflow-hidden cursor-pointer"
          onClick={() => document.getElementById("image-upload")?.click()}
        >
          {preview ? (
            <img src={preview} alt="미리보기" className="w-full h-full object-cover" />
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
        {errors.image && <p className="text-xs text-[var(--color-auction-canceled-text)]">{errors.image.message as string}</p>}
      </div>

      {/* 기본 정보 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="title">경매 제목</Label>
        <Input id="title" {...register("title")} placeholder="경매의 제목을 입력하세요" />
        {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
      </div>

      {/* 가격 및 단위 설정 구역 */}
      <div className="grid grid-cols-2 gap-4">
        {/* 시작가 */}
        <div className="flex flex-col gap-1.5">
          <Label>시작가 (<PriceDisplay amount={watch("startPrice") || 0} />)</Label>
          <Input 
            type="number" 
            {...register("startPrice")} 
            placeholder="0" // 흐릿하게 0 표시
            className="[appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
          />
        </div>

        {/* 입찰 단위 */}
        <div className="flex flex-col gap-1.5">
          <Label>입찰 단위 (<PriceDisplay amount={watch("bidUnit") || 0} />)</Label>
          <Input 
            type="number" 
            step="100" 
            min="100" // 💡 최소값을 100으로 설정하여 그 미만으로 내려가지 않게 함
            {...register("bidUnit")} 
            placeholder="100" 
          />
          {errors.bidUnit && (
            <p className="text-xs text-destructive">
              {errors.bidUnit.message as string}
            </p>
          )}
        </div>
      </div>

      {/* 경매 기간 부분 */}
      <div className="flex flex-col gap-1.5">
        <Label>경매 기간 (1~10일)</Label>
        <Input 
          type="number" 
          min="1" // 💡 최소값을 1로 설정하여 0이나 마이너스가 되지 않게 함
          max="10" // 팀 규칙에 따라 최대값도 10으로 설정
          {...register("duration")} 
        />
        {errors.duration && <p className="text-xs text-destructive">{errors.duration.message}</p>}
      </div>

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
        {isPending ? "등록 중..." : "경매 등록하기"}
      </Button>
    </form>
  );
}

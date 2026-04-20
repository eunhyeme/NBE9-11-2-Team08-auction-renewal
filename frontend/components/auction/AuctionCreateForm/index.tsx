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
import { toast } from "sonner";

export function AuctionCreateForm() {
  const router = useRouter();
  const { mutateAsync: server_createAuction, isPending } = useCreateAuction();
  const [preview, setPreview] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    setError,      // 💡 수동 에러 설정을 위해 추가
    clearErrors,   // 💡 에러 해제를 위해 추가
    formState: { errors },
  } = useForm<AuctionFormValues>({
    resolver: zodResolver(auctionSchema) as any,
    defaultValues: { 
      title: "",
      description: "",
      duration: 1 
    },
  });

  const onSubmit = async (data: AuctionFormValues) => {
    // 💡 등록 시에는 이미지가 필수이므로 수동 검증 진행
    if (!data.image) {
      setError("image", { type: "manual", message: "이미지 파일을 업로드해 주세요." });
      return;
    }

    try {
      const result = await server_createAuction(data);
      toast.success("경매가 성공적으로 등록되었습니다!");
      router.push(`/auctions/${result.auctionId}`);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.";
      toast.error(`경매 등록 실패: ${errorMessage}`);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6 max-w-xl mx-auto" noValidate>
      {/* 이미지 미리보기 구역 */}
      <div className="flex flex-col gap-2">
        <Label>상품 이미지</Label>
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
              clearErrors("image"); // 💡 이미지 선택 시 즉시 에러 메시지 제거
            }
          }}
        />
        {/* 💡 에러 메시지 표시 */}
        {errors.image && (
          <p className="text-xs text-[var(--color-auction-canceled-text)]">
            {errors.image.message as string}
          </p>
        )}
      </div>

      {/* ... 이하 기존 필드들 동일 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="title">경매 제목</Label>
        <Input id="title" {...register("title")} placeholder="경매의 제목을 입력하세요" />
        {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1.5">
          <Label>시작가 (<PriceDisplay amount={watch("startPrice") || 0} />)</Label>
          <Input 
            type="number" 
            {...register("startPrice")} 
            placeholder="0"
            className="[appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <Label>입찰 단위 (<PriceDisplay amount={watch("bidUnit") || 0} />)</Label>
          <Input 
            type="number" 
            step="100" 
            {...register("bidUnit")} 
            placeholder="100" 
          />
          {errors.bidUnit && <p className="text-xs text-destructive">{errors.bidUnit.message as string}</p>}
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label>경매 기간 (1~10일)</Label>
        <Input type="number" min="1" max="10" {...register("duration")} />
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

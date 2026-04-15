"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

import {
  auctionCreateSchema,
  type AuctionCreateFormValues,
} from "@/lib/validations/auctionSchema";
import { createAuction } from "@/lib/api/auction";
import { ApiError } from "@/types/api";

export function AuctionCreateForm() {
  const router = useRouter();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } = useForm<AuctionCreateFormValues>({
    resolver: zodResolver(auctionCreateSchema) as any,
  });

  const onSubmit = async (values: AuctionCreateFormValues) => {
    try {
      const auction = await createAuction({
        title: values.title,
        description: values.description ?? null,
        imageUrl: values.imageUrl || null,
        startPrice: values.startPrice,
        bidUnit: values.bidUnit,
        startedAt: new Date(values.startedAt).toISOString(),
        endedAt: new Date(values.endedAt).toISOString(),
      });
      toast.success("경매가 등록되었습니다.");
      router.push(`/auctions/${auction.auctionId}`);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("경매 등록 중 오류가 발생했습니다.");
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
      {/* 상품 이미지 URL */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="imageUrl">이미지 URL (선택)</Label>
        <Input
          id="imageUrl"
          type="url"
          placeholder="https://example.com/image.jpg"
          {...register("imageUrl")}
        />
        {errors.imageUrl && (
          <p className="text-xs text-destructive">{errors.imageUrl.message}</p>
        )}
      </div>

      {/* 제목 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="title">상품명</Label>
        <Input
          id="title"
          placeholder="상품명을 입력해 주세요"
          {...register("title")}
        />
        {errors.title && (
          <p className="text-xs text-destructive">{errors.title.message}</p>
        )}
      </div>

      {/* 설명 */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">상품 설명</Label>
        <Textarea
          id="description"
          placeholder="상품에 대한 설명을 입력해 주세요"
          rows={4}
          {...register("description")}
        />
      </div>

      {/* 시작가 / 입찰 단위 */}
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="startPrice">시작가 (원)</Label>
          <Input
            id="startPrice"
            type="number"
            placeholder="1000"
            {...register("startPrice")}
          />
          {errors.startPrice && (
            <p className="text-xs text-destructive">{errors.startPrice.message}</p>
          )}
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="bidUnit">입찰 단위 (원)</Label>
          <Input
            id="bidUnit"
            type="number"
            placeholder="100"
            {...register("bidUnit")}
          />
          {errors.bidUnit && (
            <p className="text-xs text-destructive">{errors.bidUnit.message}</p>
          )}
        </div>
      </div>

      {/* 시작/종료 시간 */}
      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="startedAt">시작 시간</Label>
          <Input
            id="startedAt"
            type="datetime-local"
            {...register("startedAt")}
          />
          {errors.startedAt && (
            <p className="text-xs text-destructive">{errors.startedAt.message}</p>
          )}
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="endedAt">종료 시간</Label>
          <Input
            id="endedAt"
            type="datetime-local"
            {...register("endedAt")}
          />
          {errors.endedAt && (
            <p className="text-xs text-destructive">{errors.endedAt.message}</p>
          )}
        </div>
      </div>

      <Button type="submit" size="lg" disabled={isSubmitting} className="mt-2">
        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        상품 등록
      </Button>
    </form>
  );
}

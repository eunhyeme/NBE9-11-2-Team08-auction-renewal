"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, ShoppingCart, Truck, PackageX, Ban } from "lucide-react";
import { toast } from "sonner";

import Link from "next/link";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogClose,
  DialogFooter,
} from "@/components/ui/dialog";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { Separator } from "@/components/ui/separator";

import { useAuthStore } from "@/lib/auth/store";
import { usePlaceBid } from "@/lib/hooks/usePlaceBid";
import { calcMinBid } from "@/lib/utils/auctionHelpers";
import { formatPrice } from "@/lib/utils/formatPrice";
import { ApiError } from "@/types/api";
import type { AuctionResponse } from "@/types/auction";

// ─── ONGOING: 입찰 폼 ─────────────────────────────────────────────
function BidForm({ auction }: { auction: AuctionResponse }) {
  const router = useRouter();
  const { isLoggedIn, userId } = useAuthStore();
  const [open, setOpen] = useState(false);
  const [pendingAmount, setPendingAmount] = useState(0);
  const placeBid = usePlaceBid(auction.auctionId);

  const minBid = calcMinBid(auction.currentPrice, auction.bidUnit);

  const bidSchema = z.object({
    bidPrice: z.coerce
      .number()
      .min(minBid, { error: `최소 ${formatPrice(minBid)} 이상 입력해 주세요.` }),
  });
  type BidForm = z.infer<typeof bidSchema>;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } = useForm<BidForm>({ resolver: zodResolver(bidSchema) as any });

  const onConfirm = async () => {
    if (!userId) return;
    try {
      await placeBid.mutateAsync({
        auctionId: auction.auctionId,
        bidderId: userId,
        bidPrice: pendingAmount,
      });
      toast.success(`${formatPrice(pendingAmount)}로 입찰했습니다.`);
      setOpen(false);
      reset();
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("입찰 중 오류가 발생했습니다.");
      }
      setOpen(false);
    }
  };

  if (!isLoggedIn) {
    return (
      <div className="flex flex-col gap-3 rounded-xl border bg-card p-5">
        <p className="text-sm text-muted-foreground">
          입찰하려면 로그인이 필요합니다.
        </p>
        <Button onClick={() => router.push("/login")}>로그인</Button>
      </div>
    );
  }

  return (
    <>
      <div className="flex flex-col gap-4 rounded-xl border bg-card p-5">
        <div className="flex justify-between items-end">
          <PriceDisplay amount={auction.currentPrice} label="현재가" size="lg" />
          <div className="text-right">
            <p className="text-xs text-muted-foreground">최소 입찰가</p>
            <p className="font-semibold text-[var(--color-brand-600)]">
              {formatPrice(minBid)}
            </p>
          </div>
        </div>

        <Separator />

        <form
          onSubmit={handleSubmit((values) => {
            setPendingAmount(values.bidPrice);
            setOpen(true);
          })}
          className="flex flex-col gap-3"
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="bidPrice">입찰 금액</Label>
            <div className="flex gap-2">
              <Input
                id="bidPrice"
                type="number"
                placeholder={String(minBid)}
                {...register("bidPrice")}
              />
              <Button type="submit" className="shrink-0">
                입찰하기
              </Button>
            </div>
            {errors.bidPrice && (
              <p className="text-xs text-destructive">{errors.bidPrice.message}</p>
            )}
          </div>
        </form>
      </div>

      {/* 확인 Dialog */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>입찰 확인</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            <span className="font-semibold text-foreground">
              {formatPrice(pendingAmount)}
            </span>
            으로 입찰하시겠습니까?
          </p>
          <DialogFooter className="flex gap-2">
            <DialogClose render={<Button variant="outline" className="flex-1" />}>
              취소
            </DialogClose>
            <Button
              onClick={onConfirm}
              disabled={placeBid.isPending}
              className="flex-1"
            >
              {placeBid.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              확인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

// ─── 메인 BidPanel ────────────────────────────────────────────────
interface BidPanelProps {
  auction: AuctionResponse;
}

export function BidPanel({ auction }: BidPanelProps) {
  const { userId } = useAuthStore();

  switch (auction.status) {
    case "ONGOING":
      return <BidForm auction={auction} />;

    case "WON": {
      const isWinner = userId === auction.lastBidderId;
      const isSeller = userId === auction.sellerId;

      if (isWinner) {
        return (
          <div className="flex flex-col gap-4 rounded-xl border bg-card p-5">
            <div className="flex items-center gap-2 text-[var(--color-auction-won-text)]">
              <ShoppingCart className="h-5 w-5" />
              <p className="font-semibold">낙찰되었습니다!</p>
            </div>
            <PriceDisplay
              amount={auction.currentPrice}
              label="낙찰가"
              size="lg"
            />
            <p className="text-xs text-muted-foreground">
              마이페이지에서 결제를 진행해 주세요.
            </p>
            <Link href="/profile" className={buttonVariants({ variant: "default" })}>
              마이페이지에서 결제하기
            </Link>
          </div>
        );
      }

      if (isSeller) {
        return (
          <div className="flex flex-col gap-3 rounded-xl border bg-card p-5">
            <div className="flex items-center gap-2 text-[var(--color-auction-won-text)]">
              <Truck className="h-5 w-5" />
              <p className="font-semibold">낙찰 완료</p>
            </div>
            <p className="text-sm text-muted-foreground">
              구매자의 결제를 기다리고 있습니다.
              <br />
              결제 완료 후 배송을 진행해 주세요.
            </p>
          </div>
        );
      }

      return (
        <div className="rounded-xl border bg-muted/50 p-5 text-center text-sm text-muted-foreground">
          낙찰이 완료된 경매입니다.
        </div>
      );
    }

    case "UNSOLD":
      return (
        <div className="flex flex-col items-center gap-2 rounded-xl border bg-muted/50 p-5 text-center">
          <PackageX className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm font-medium">유찰된 경매입니다</p>
          <p className="text-xs text-muted-foreground">
            입찰자가 없어 경매가 취소되었습니다.
          </p>
        </div>
      );

    case "CANCELED":
      return (
        <div className="flex flex-col items-center gap-2 rounded-xl border bg-muted/50 p-5 text-center">
          <Ban className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm font-medium">취소된 경매입니다</p>
        </div>
      );
  }
}

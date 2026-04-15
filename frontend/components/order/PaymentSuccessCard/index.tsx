import { CheckCircle } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { cn } from "@/lib/utils";

interface PaymentSuccessCardProps {
  orderId: number;
  finalPrice: number;
  auctionTitle?: string;
}

export function PaymentSuccessCard({
  orderId,
  finalPrice,
  auctionTitle,
}: PaymentSuccessCardProps) {
  return (
    <div className="flex flex-col items-center gap-6 rounded-xl border bg-card p-8 text-center">
      <div className="flex flex-col items-center gap-3">
        <CheckCircle className="h-16 w-16 text-[var(--color-auction-ongoing-text)]" />
        <h1 className="text-2xl font-bold">결제 완료</h1>
        <p className="text-sm text-muted-foreground">
          결제가 성공적으로 처리되었습니다.
        </p>
      </div>

      <div className="w-full rounded-lg bg-muted/50 p-4 flex flex-col gap-3">
        {auctionTitle && (
          <p className="font-medium text-sm line-clamp-2">{auctionTitle}</p>
        )}
        <div className="flex justify-between items-center text-sm">
          <span className="text-muted-foreground">주문번호</span>
          <span className="font-mono">#{orderId}</span>
        </div>
        <PriceDisplay amount={finalPrice} label="결제 금액" size="lg" />
      </div>

      <p className="text-xs text-muted-foreground">
        판매자가 배송을 준비합니다. 마이페이지에서 배송 상태를 확인하세요.
      </p>

      <div className="flex flex-col sm:flex-row gap-3 w-full">
        <Link
          href="/profile"
          className={cn(buttonVariants({ variant: "default" }), "flex-1")}
        >
          마이페이지에서 확인
        </Link>
        <Link
          href="/"
          className={cn(buttonVariants({ variant: "outline" }), "flex-1")}
        >
          경매 목록으로
        </Link>
      </div>
    </div>
  );
}

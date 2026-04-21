"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ShieldX } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { AuctionCreateForm } from "@/components/auction/AuctionCreateForm";
import { useAuthStore } from "@/lib/auth/store";
import { cn } from "@/lib/utils";

export default function AuctionNewPage() {
  const router = useRouter();
  const { isLoggedIn, permissions } = useAuthStore();
  const isSeller = permissions.includes("SELLER");

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace("/login");
    }
  }, [isLoggedIn, router]);

  if (!isLoggedIn) return null;

  if (!isSeller) {
    return (
      <div className="flex flex-col items-center justify-center gap-6 py-24 text-center">
        <ShieldX className="h-16 w-16 text-muted-foreground" />
        <div className="flex flex-col gap-2">
          <h1 className="text-2xl font-bold">판매자 인증이 필요합니다</h1>
          <p className="text-muted-foreground text-sm">
            경매 글을 작성하려면 판매자 인증이 필요합니다.
            <br />
            프로필 페이지에서 판매자 인증을 신청해 주세요.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => router.back()}>
            이전으로
          </Button>
          <Link href="/" className={cn(buttonVariants())}>
            메인으로
          </Link>
          <Link href="/profile" className={cn(buttonVariants({ variant: "outline" }))}>
            판매자 인증 신청
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold">경매 등록</h1>
        <p className="text-sm text-muted-foreground mt-1">
          판매할 상품의 정보를 입력해 주세요.
        </p>
      </div>
      <AuctionCreateForm />
    </div>
  );
}

"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuctionCreateForm } from "@/components/auction/AuctionCreateForm";
import { useAuthStore } from "@/lib/auth/store";

export default function AuctionNewPage() {
  const router = useRouter();
  const { isLoggedIn, hasBluecheck } = useAuthStore();

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace("/login");
      return;
    }
    if (!hasBluecheck) {
      router.replace("/profile");
    }
  }, [isLoggedIn, hasBluecheck, router]);

  if (!isLoggedIn || !hasBluecheck) return null;

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

"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Settings, BadgePlus } from "lucide-react";
import { cn } from "@/lib/utils";
import { buttonVariants } from "@/components/ui/button";
import { toast } from "sonner";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MembershipBadge } from "@/components/user/MembershipBadge";
import { OrderHistoryItem } from "@/components/order/OrderHistoryItem";
import { OrderFilterTabs, type OrderFilter } from "@/components/mypage/OrderFilterTabs";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";

import { useAuthStore } from "@/lib/auth/store";
import { useMe } from "@/lib/hooks/useMe";
import { useApplyMembership } from "@/lib/hooks/useMembership";
import type { OrderResponse, OrderStatus } from "@/types/order";

// ─── 필터 → OrderStatus 매핑 ──────────────────────────────────────
const FILTER_STATUS_MAP: Record<OrderFilter, OrderStatus[] | null> = {
  all:     null, // 전체
  ongoing: ["AWAITING_PAYMENT", "DELIVERING"],
  bidding: [], // 경매 ONGOING 중인 주문 — 임시로 빈 배열 (백엔드 지원 시 연동)
  done:    ["PAID", "DELIVERED", "NOT_PAID", "NOT_DELIVERED"],
};

function filterOrders(orders: OrderResponse[], filter: OrderFilter): OrderResponse[] {
  const allowed = FILTER_STATUS_MAP[filter];
  if (!allowed) return orders;
  return orders.filter((o) => allowed.includes(o.status));
}

// ─── 주문 목록 서브 컴포넌트 ──────────────────────────────────────
interface OrderListProps {
  orders: OrderResponse[];
  filter: OrderFilter;
  onFilterChange: (f: OrderFilter) => void;
  emptyMessage: string;
}

function OrderList({ orders, filter, onFilterChange, emptyMessage }: OrderListProps) {
  const filtered = filterOrders(orders, filter);

  return (
    <div className="flex flex-col gap-3">
      <OrderFilterTabs value={filter} onChange={onFilterChange} />
      {filtered.length === 0 ? (
        <EmptyState message={emptyMessage} />
      ) : (
        <div className="flex flex-col gap-2">
          {filtered.map((order) => (
            <OrderHistoryItem key={order.orderId} order={order} />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── 페이지 ──────────────────────────────────────────────────────
export default function ProfilePage() {
  const router = useRouter();
  const { isLoggedIn, nickname, hasBluecheck, role } = useAuthStore();
  const [purchaseFilter, setPurchaseFilter] = useState<OrderFilter>("all");
  const [salesFilter, setSalesFilter] = useState<OrderFilter>("all");

  const { data: me, isLoading, isError } = useMe();
  const applyMembership = useApplyMembership();

  if (!isLoggedIn) {
    router.replace("/login");
    return null;
  }

  if (isLoading) {
    return <LoadingSpinner className="py-24" size="lg" />;
  }

  if (isError) {
    return <ErrorMessage message="프로필을 불러오는 데 실패했습니다." />;
  }

  const handleApplyMembership = async () => {
    try {
      await applyMembership.mutateAsync();
      toast.success("판매자 인증 신청이 완료되었습니다.");
    } catch {
      toast.error("이미 신청하셨거나 오류가 발생했습니다.");
    }
  };

  // TODO: 백엔드에서 주문 목록 API 연동 시 실제 데이터로 교체
  const purchaseOrders: OrderResponse[] = [];
  const salesOrders: OrderResponse[] = [];

  return (
    <div className="flex flex-col gap-6">
      {/* ── 프로필 카드 ── */}
      <div className="rounded-xl border bg-card p-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          {/* 아바타 */}
          <Avatar className="h-16 w-16">
            <AvatarFallback className="text-2xl">
              {nickname?.[0]?.toUpperCase() ?? "U"}
            </AvatarFallback>
          </Avatar>

          {/* 정보 */}
          <div className="flex-1 flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <span className="text-xl font-bold">{me?.nickname ?? nickname}</span>
              {hasBluecheck && (
                <MembershipBadge className="h-5 w-5" />
              )}
            </div>
            <span className="text-sm text-muted-foreground">{me?.email}</span>
            {me?.address && (
              <span className="text-sm text-muted-foreground">{me.address}</span>
            )}
          </div>

          {/* 액션 버튼 */}
          <div className="flex flex-col gap-2 sm:items-end">
            <Link
              href="/profile/edit"
              className={cn(buttonVariants({ variant: "outline", size: "sm" }), "gap-1.5")}
            >
              <Settings className="h-4 w-4" />
              프로필 수정
            </Link>
            {!hasBluecheck && role !== "ROLE_ADMIN" && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleApplyMembership}
                disabled={applyMembership.isPending}
              >
                <BadgePlus className="h-4 w-4 mr-1.5" />
                판매자 인증 신청
              </Button>
            )}
          </div>
        </div>
      </div>

      <Separator />

      {/* ── 구매/판매 내역 탭 ── */}
      <Tabs defaultValue="purchase">
        <TabsList className="w-full max-w-xs">
          <TabsTrigger value="purchase" className="flex-1">구매내역</TabsTrigger>
          <TabsTrigger value="sales" className="flex-1">판매내역</TabsTrigger>
        </TabsList>

        {/* 구매내역 */}
        <TabsContent value="purchase" className="mt-4">
          <OrderList
            orders={purchaseOrders}
            filter={purchaseFilter}
            onFilterChange={setPurchaseFilter}
            emptyMessage="구매 내역이 없습니다."
          />
        </TabsContent>

        {/* 판매내역 */}
        <TabsContent value="sales" className="mt-4">
          <OrderList
            orders={salesOrders}
            filter={salesFilter}
            onFilterChange={setSalesFilter}
            emptyMessage="판매 내역이 없습니다."
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

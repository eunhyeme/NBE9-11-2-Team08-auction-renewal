"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
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
import { AuctionStatusBadge } from "@/components/auction/AuctionStatusBadge";
import { PriceDisplay } from "@/components/common/PriceDisplay";
import { OrderFilterTabs, type OrderFilter } from "@/components/mypage/OrderFilterTabs";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";

import { useAuthStore } from "@/lib/auth/store";
import { useMe } from "@/lib/hooks/useMe";
import { useApplyMembership } from "@/lib/hooks/useMembership";
import { useMyOrders } from "@/lib/hooks/useOrder";
import { useMyBids } from "@/lib/hooks/useBids";
import { useMyAuctions } from "@/lib/hooks/useAuctions";
import { formatDate } from "@/lib/utils/formatDate";
import { resolveImageUrl } from "@/lib/utils/resolveImageUrl";
import { ApiError } from "@/types/api";
import type { AuctionResponse } from "@/types/auction";
import type { MyBidResponse } from "@/types/bid";
import type { OrderResponse } from "@/types/order";

// ─── 주문 목록 서브 컴포넌트 ──────────────────────────────────────
type HistoryTab = "purchase" | "sales";
type HistoryItem =
  | { type: "order"; order: OrderResponse }
  | { type: "bid"; bid: MyBidResponse }
  | { type: "auction"; auction: AuctionResponse };

interface HistoryListProps {
  items: HistoryItem[];
  filter: OrderFilter;
  onFilterChange: (f: OrderFilter) => void;
  emptyMessages: Record<OrderFilter, string>;
  isLoading?: boolean;
  isError?: boolean;
}

function HistoryList({
  items,
  filter,
  onFilterChange,
  emptyMessages,
  isLoading,
  isError,
}: HistoryListProps) {
  return (
    <div className="flex flex-col gap-3">
      <OrderFilterTabs value={filter} onChange={onFilterChange} />
      {isLoading ? (
        <LoadingSpinner className="py-12" />
      ) : isError ? (
        <ErrorMessage message="내역을 불러오는 데 실패했습니다." />
      ) : items.length === 0 ? (
        <EmptyState message={emptyMessages[filter]} />
      ) : (
        <div className="flex flex-col gap-2">
          {items.map((item) => (
            <HistoryListItem key={getHistoryItemKey(item)} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}

function getHistoryItemKey(item: HistoryItem) {
  if (item.type === "order") return `order-${item.order.orderId}`;
  if (item.type === "bid") return `bid-${item.bid.bidId}`;
  return `auction-${item.auction.auctionId}`;
}

function HistoryListItem({ item }: { item: HistoryItem }) {
  if (item.type === "order") {
    return <OrderHistoryItem order={item.order} />;
  }

  if (item.type === "bid") {
    return (
      <AuctionHistoryLink
        href={`/auctions/${item.bid.auctionId}`}
        title={item.bid.auctionTitle}
        imageUrl={item.bid.auctionImageUrl}
        status={item.bid.auctionStatus}
        date={item.bid.bidAt}
        amount={item.bid.myBidPrice}
        priceLabel="내 입찰가"
      />
    );
  }

  return (
    <AuctionHistoryLink
      href={`/profile/auctions/${item.auction.auctionId}`}
      title={item.auction.title}
      imageUrl={item.auction.imageUrl}
      status={item.auction.status}
      date={item.auction.startedAt}
      amount={item.auction.currentPrice}
      priceLabel="현재가"
    />
  );
}

interface AuctionHistoryLinkProps {
  href: string;
  title: string;
  imageUrl: string | null;
  status: AuctionResponse["status"];
  date: string;
  amount: number;
  priceLabel: string;
}

function AuctionHistoryLink({
  href,
  title,
  imageUrl,
  status,
  date,
  amount,
  priceLabel,
}: AuctionHistoryLinkProps) {
  const resolvedImageUrl = resolveImageUrl(imageUrl);

  return (
    <Link href={href}>
      <div className="flex items-center gap-4 rounded-lg border bg-card p-4 hover:bg-muted/50 transition-colors">
        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-md bg-muted">
          {resolvedImageUrl ? (
            <Image
              src={resolvedImageUrl}
              alt={title}
              fill
              className="object-cover"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-muted-foreground text-xs">
              No Image
            </div>
          )}
        </div>

        <div className="flex flex-1 flex-col gap-1 min-w-0">
          <p className="font-medium truncate">{title}</p>
          <div className="flex items-center gap-2">
            <AuctionStatusBadge status={status} />
            <span className="text-xs text-muted-foreground">
              {formatDate(date)}
            </span>
          </div>
        </div>

        <PriceDisplay amount={amount} label={priceLabel} />
      </div>
    </Link>
  );
}

// ─── 페이지 ──────────────────────────────────────────────────────
export default function ProfilePage() {
  const router = useRouter();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const [historyTab, setHistoryTab] = useState<HistoryTab>("purchase");
  const [purchaseFilter, setPurchaseFilter] = useState<OrderFilter>("bidding");
  const [salesFilter, setSalesFilter] = useState<OrderFilter>("bidding");

  const { data: me, isLoading, isError } = useMe();
  const applyMembership = useApplyMembership();
  const purchaseBids = useMyBids(
    isLoggedIn && historyTab === "purchase" && purchaseFilter === "bidding",
  );
  const purchaseOngoingOrders = useMyOrders(
    "BUYER",
    "ONGOING",
    isLoggedIn && historyTab === "purchase" && purchaseFilter === "ongoing",
  );
  const purchaseDoneOrders = useMyOrders(
    "BUYER",
    "COMPLETED",
    isLoggedIn && historyTab === "purchase" && purchaseFilter === "done",
  );
  const salesAuctions = useMyAuctions(
    isLoggedIn && historyTab === "sales" && salesFilter === "bidding",
  );
  const salesOngoingOrders = useMyOrders(
    "SELLER",
    "ONGOING",
    isLoggedIn && historyTab === "sales" && salesFilter === "ongoing",
  );
  const salesDoneOrders = useMyOrders(
    "SELLER",
    "COMPLETED",
    isLoggedIn && historyTab === "sales" && salesFilter === "done",
  );

  useEffect(() => {
    if (!isLoggedIn) router.replace("/login");
  }, [isLoggedIn, router]);

  if (!isLoggedIn) return null;

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
    } catch (err) {
      if (
        err instanceof ApiError &&
        (err.code === "400-3" || err.message === "이미 신청이 완료되었습니다.")
      ) {
        toast.error("이미 신청이 완료되었습니다.");
      } else {
        toast.error("이미 신청하셨거나 오류가 발생했습니다.");
      }
    }
  };

  const purchaseItems: HistoryItem[] =
    purchaseFilter === "bidding"
      ? (purchaseBids.data ?? []).map((bid) => ({ type: "bid", bid }))
      : purchaseFilter === "ongoing"
        ? (purchaseOngoingOrders.data ?? []).map((order) => ({ type: "order", order }))
        : (purchaseDoneOrders.data ?? []).map((order) => ({ type: "order", order }));
  const salesItems: HistoryItem[] =
    salesFilter === "bidding"
      ? (salesAuctions.data ?? []).map((auction) => ({ type: "auction", auction }))
      : salesFilter === "ongoing"
        ? (salesOngoingOrders.data ?? []).map((order) => ({ type: "order", order }))
        : (salesDoneOrders.data ?? []).map((order) => ({ type: "order", order }));
  const purchaseHistoryState =
    purchaseFilter === "bidding"
      ? purchaseBids
      : purchaseFilter === "ongoing"
        ? purchaseOngoingOrders
        : purchaseDoneOrders;
  const salesHistoryState =
    salesFilter === "bidding"
      ? salesAuctions
      : salesFilter === "ongoing"
        ? salesOngoingOrders
        : salesDoneOrders;
  const canApplyMembership = !me?.hasBluecheck && me?.role !== "ROLE_ADMIN";

  return (
    <div className="flex flex-col gap-6">
      {/* ── 프로필 카드 ── */}
      <div className="rounded-xl border bg-card p-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          {/* 아바타 */}
          <Avatar className="h-16 w-16">
            <AvatarFallback className="text-2xl">
              {me?.nickname?.[0]?.toUpperCase() ?? "U"}
            </AvatarFallback>
          </Avatar>

          {/* 정보 */}
          <div className="flex-1 flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <span className="text-xl font-bold">{me?.nickname}</span>
              {me?.hasBluecheck && (
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
            {canApplyMembership && (
              <div className="flex flex-col gap-1 sm:items-end">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleApplyMembership}
                  disabled={applyMembership.isPending}
                >
                  <BadgePlus className="h-4 w-4 mr-1.5" />
                  판매자 인증 신청
                </Button>
                <p className="text-xs text-muted-foreground">
                  인증 신청 후 관리자 승인까지 기다려 주세요.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      <Separator />

      {/* ── 구매/판매 내역 탭 ── */}
      <Tabs value={historyTab} onValueChange={(v) => setHistoryTab(v as HistoryTab)}>
        <TabsList className="w-full max-w-xs">
          <TabsTrigger value="purchase" className="flex-1">구매내역</TabsTrigger>
          <TabsTrigger value="sales" className="flex-1">판매내역</TabsTrigger>
        </TabsList>

        {/* 구매내역 */}
        <TabsContent value="purchase" className="mt-4">
          <HistoryList
            items={purchaseItems}
            filter={purchaseFilter}
            onFilterChange={setPurchaseFilter}
            isLoading={purchaseHistoryState.isLoading}
            isError={purchaseHistoryState.isError}
            emptyMessages={{
              bidding: "입찰 내역이 없습니다.",
              ongoing: "진행중인 구매 내역이 없습니다.",
              done: "종료된 구매 내역이 없습니다.",
            }}
          />
        </TabsContent>

        {/* 판매내역 */}
        <TabsContent value="sales" className="mt-4">
          <HistoryList
            items={salesItems}
            filter={salesFilter}
            onFilterChange={setSalesFilter}
            isLoading={salesHistoryState.isLoading}
            isError={salesHistoryState.isError}
            emptyMessages={{
              bidding: "등록한 경매 내역이 없습니다.",
              ongoing: "진행중인 판매 내역이 없습니다.",
              done: "종료된 판매 내역이 없습니다.",
            }}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

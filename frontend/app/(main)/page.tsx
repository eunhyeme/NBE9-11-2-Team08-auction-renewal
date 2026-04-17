"use client";

import { useState } from "react";
import Link from "next/link";
import { Search, PlusCircle, BadgeCheck } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import { AuctionGrid } from "@/components/auction/AuctionGrid";
import { useAuthStore } from "@/lib/auth/store";
import { cn } from "@/lib/utils";

const SORT_OPTIONS = [
  { label: "남은시간 순", key: "endingSoon", sort: "endedAt,asc" },
  { label: "신규 등록순", key: "newest", sort: "startedAt,desc" },
  { label: "가격 높은순", key: "priceDesc", sort: "currentPrice,desc" },
  { label: "가격 낮은순", key: "priceAsc", sort: "currentPrice,asc" },
];

const PRICE_OPTIONS = [
  { label: "전체 가격", value: "" },
  { label: "5만원 이하", value: "0-50000" },
  { label: "5만 ~ 20만원", value: "50000-200000" },
  { label: "20만 ~ 50만원", value: "200000-500000" },
  { label: "50만원 이상", value: "500000-" },
];

function parsePriceRange(value: string): { minPrice?: number; maxPrice?: number } {
  if (!value) return {};
  const [min, max] = value.split("-");
  return {
    minPrice: min ? Number(min) : undefined,
    maxPrice: max ? Number(max) : undefined,
  };
}

export default function HomePage() {
  const [inputValue, setInputValue] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sortKey, setSortKey] = useState("newest");
  const sort = SORT_OPTIONS.find((o) => o.key === sortKey)?.sort ?? "startedAt,desc";
  const [priceRange, setPriceRange] = useState("");
  const [bluecheckOnly, setBluecheckOnly] = useState(false);
  const { isLoggedIn } = useAuthStore();

  const { minPrice, maxPrice } = parsePriceRange(priceRange);

  const handleSearch = () => {
    setKeyword(inputValue.trim());
  };

  return (
    <div className="flex flex-col gap-6">
      {/* 타이틀 + 판매 등록 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">진행 중인 경매</h1>
        {isLoggedIn && (
          <Link
            href="/auctions/new"
            className={cn(buttonVariants({ size: "sm" }), "gap-1.5")}
          >
            <PlusCircle className="h-4 w-4" />
            판매 등록
          </Link>
        )}
      </div>

      {/* 검색 */}
      <div className="flex gap-2">
        <Input
          placeholder="경매 상품을 검색하세요"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          className="max-w-sm"
        />
        <Button onClick={handleSearch} variant="outline" size="sm">
          <Search className="h-4 w-4 mr-1.5" />
          검색
        </Button>
      </div>

      {/* 필터 + 정렬 */}
      <div className="flex items-center justify-between gap-2 flex-wrap">
        {/* 왼쪽: 필터 */}
        <div className="flex items-center gap-2 flex-wrap">
          <Select value={priceRange} onValueChange={(v) => setPriceRange(v ?? "")}>
            <SelectTrigger size="sm" className="w-36">
              {PRICE_OPTIONS.find((o) => o.value === priceRange)?.label ?? "가격"}
            </SelectTrigger>
            <SelectContent>
              {PRICE_OPTIONS.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Button
            variant={bluecheckOnly ? "default" : "outline"}
            size="sm"
            onClick={() => setBluecheckOnly((v) => !v)}
            className="gap-1.5"
          >
            <BadgeCheck className="h-4 w-4" />
            블루체크 판매자
          </Button>
        </div>

        {/* 오른쪽: 정렬 */}
        <Select value={sortKey} onValueChange={(v) => setSortKey(v ?? "newest")}>
          <SelectTrigger size="sm" className="w-36">
            {SORT_OPTIONS.find((o) => o.key === sortKey)?.label ?? "정렬"}
          </SelectTrigger>
          <SelectContent align="end">
            {SORT_OPTIONS.map((opt) => (
              <SelectItem key={opt.key} value={opt.key}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <AuctionGrid
        keyword={keyword}
        sort={sort}
        minPrice={minPrice}
        maxPrice={maxPrice}
        bluecheckOnly={bluecheckOnly}
      />
    </div>
  );
}

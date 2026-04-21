"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { Search, PlusCircle, ChevronDown } from "lucide-react";
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

type AuctionStatusFilter = "ONGOING" | "WON" | "UNSOLD";

const STATUS_OPTIONS: { label: string; value: AuctionStatusFilter }[] = [
  { label: "경매 중", value: "ONGOING" },
  { label: "낙찰", value: "WON" },
  { label: "유찰", value: "UNSOLD" },
];

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
  const [minPriceInput, setMinPriceInput] = useState("");
  const [maxPriceInput, setMaxPriceInput] = useState("");
  const [debouncedMin, setDebouncedMin] = useState("");
  const [debouncedMax, setDebouncedMax] = useState("");
  const [priceOpen, setPriceOpen] = useState(false);
  const priceRef = useRef<HTMLDivElement>(null);
  const [statusFilters, setStatusFilters] = useState<AuctionStatusFilter[]>(["ONGOING"]);

  const toggleStatus = (status: AuctionStatusFilter) => {
    setStatusFilters((prev) => {
      const next = prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status];
      return next.length === 0 ? ["ONGOING"] : next;
    });
  };
  const { isLoggedIn } = useAuthStore();

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (priceRef.current && !priceRef.current.contains(e.target as Node)) {
        setPriceOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedMin(minPriceInput), 500);
    return () => clearTimeout(timer);
  }, [minPriceInput]);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedMax(maxPriceInput), 500);
    return () => clearTimeout(timer);
  }, [maxPriceInput]);

  const { minPrice: selectMin, maxPrice: selectMax } = parsePriceRange(priceRange);
  const rawMin = debouncedMin ? Number(debouncedMin) : selectMin;
  const rawMax = debouncedMax ? Number(debouncedMax) : selectMax;
  const minPrice = rawMin !== undefined && rawMax !== undefined && rawMin > rawMax ? undefined : rawMin;
  const maxPrice = rawMin !== undefined && rawMax !== undefined && rawMin > rawMax ? undefined : rawMax;

  const priceTriggerLabel = (() => {
    if (minPriceInput || maxPriceInput) {
      const min = minPriceInput ? `${Number(minPriceInput).toLocaleString()}원` : "0원";
      const max = maxPriceInput ? `${Number(maxPriceInput).toLocaleString()}원` : "∞";
      return `${min} ~ ${max}`;
    }
    return PRICE_OPTIONS.find((o) => o.value === priceRange)?.label ?? "가격";
  })();

  const handleSearch = () => {
    setKeyword(inputValue.trim());
  };

  return (
    <div className="flex flex-col gap-6">
      {/* 타이틀 + 판매 등록 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">경매 목록</h1>
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
          {/* 상태 필터 (다중 선택) */}
          <div className="flex gap-2">
            {STATUS_OPTIONS.map((opt) => (
              <Button
                key={opt.value}
                variant={statusFilters.includes(opt.value) ? "default" : "outline"}
                size="sm"
                onClick={() => toggleStatus(opt.value)}
              >
                {opt.label}
              </Button>
            ))}
          </div>

          <div ref={priceRef} className="relative">
            <button
              type="button"
              onClick={() => setPriceOpen((o) => !o)}
              className="flex h-8 items-center gap-1.5 rounded-md border border-input bg-background px-3 text-sm shadow-xs hover:bg-accent"
            >
              <span className="max-w-36 truncate">{priceTriggerLabel}</span>
              <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
            </button>

            {priceOpen && (
              <div className="absolute left-0 top-9 z-50 w-52 rounded-md border bg-popover p-1 shadow-md">
                {PRICE_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => { setPriceRange(opt.value); setMinPriceInput(""); setMaxPriceInput(""); setDebouncedMin(""); setDebouncedMax(""); setPriceOpen(false); }}
                    className={cn(
                      "w-full rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent",
                      priceRange === opt.value && !minPriceInput && !maxPriceInput && "font-medium"
                    )}
                  >
                    {opt.label}
                  </button>
                ))}
                <div className="my-1 border-t" />
                <div className="flex items-center gap-1 px-2 py-1.5">
                  <Input
                    type="number"
                    placeholder="최저"
                    min={0}
                    value={minPriceInput}
                    onChange={(e) => { if (Number(e.target.value) >= 0) { setMinPriceInput(e.target.value); setPriceRange(""); } }}
                    className="h-7 text-xs"
                  />
                  <span className="text-muted-foreground text-xs shrink-0">~</span>
                  <Input
                    type="number"
                    placeholder="최고"
                    min={0}
                    value={maxPriceInput}
                    onChange={(e) => { if (Number(e.target.value) >= 0) { setMaxPriceInput(e.target.value); setPriceRange(""); } }}
                    className="h-7 text-xs"
                  />
                </div>
              </div>
            )}
          </div>
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
        statuses={statusFilters}
      />
    </div>
  );
}

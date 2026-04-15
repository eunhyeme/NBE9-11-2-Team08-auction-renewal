"use client";

import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

export type OrderFilter = "all" | "ongoing" | "bidding" | "done";

interface OrderFilterTabsProps {
  value: OrderFilter;
  onChange: (value: OrderFilter) => void;
}

export function OrderFilterTabs({ value, onChange }: OrderFilterTabsProps) {
  return (
    <Tabs value={value} onValueChange={(v) => onChange(v as OrderFilter)}>
      <TabsList className="w-full">
        <TabsTrigger value="all" className="flex-1">전체</TabsTrigger>
        <TabsTrigger value="ongoing" className="flex-1">진행 중</TabsTrigger>
        <TabsTrigger value="bidding" className="flex-1">입찰 중</TabsTrigger>
        <TabsTrigger value="done" className="flex-1">완료</TabsTrigger>
      </TabsList>
    </Tabs>
  );
}

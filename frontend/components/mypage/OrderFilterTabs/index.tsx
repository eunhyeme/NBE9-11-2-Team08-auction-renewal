"use client";

import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

export type OrderFilter = "bidding" | "ongoing" | "done";

interface OrderFilterTabsProps {
  value: OrderFilter;
  onChange: (value: OrderFilter) => void;
}

export function OrderFilterTabs({ value, onChange }: OrderFilterTabsProps) {
  return (
    <Tabs value={value} onValueChange={(v) => onChange(v as OrderFilter)}>
      <TabsList className="w-full">
        <TabsTrigger value="bidding" className="flex-1">입찰</TabsTrigger>
        <TabsTrigger value="ongoing" className="flex-1">진행중</TabsTrigger>
        <TabsTrigger value="done" className="flex-1">종료</TabsTrigger>
      </TabsList>
    </Tabs>
  );
}

"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { formatTimeLeft } from "@/lib/utils/auctionHelpers";
import { isPast, parseISO } from "date-fns";

interface CountdownTimerProps {
  endedAt: string;
  onExpire?: () => void;
  className?: string;
}

/**
 * 경매 종료까지 남은 시간을 실시간으로 표시한다.
 * SSR 하이드레이션 불일치를 방지하기 위해 마운트 후 렌더링한다.
 */
export function CountdownTimer({
  endedAt,
  onExpire,
  className,
}: CountdownTimerProps) {
  const [mounted, setMounted] = useState(false);
  const [timeLeft, setTimeLeft] = useState("");

  useEffect(() => {
    setMounted(true);
    setTimeLeft(formatTimeLeft(endedAt));
  }, [endedAt]);

  useEffect(() => {
    if (!mounted) return;
    const tick = () => {
      const label = formatTimeLeft(endedAt);
      setTimeLeft(label);
      if (isPast(parseISO(endedAt))) {
        onExpire?.();
        clearInterval(id);
      }
    };
    const id = setInterval(tick, 1_000);
    return () => clearInterval(id);
  }, [endedAt, mounted, onExpire]);

  if (!mounted) return null;

  const isExpired = timeLeft === "종료됨";
  return (
    <span
      className={cn(
        "text-sm font-medium",
        isExpired ? "text-muted-foreground" : "text-[var(--color-brand-600)]",
        className,
      )}
    >
      {timeLeft}
    </span>
  );
}

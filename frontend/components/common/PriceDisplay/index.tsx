import { cn } from "@/lib/utils";
import { formatPrice } from "@/lib/utils/formatPrice";

interface PriceDisplayProps {
  amount: number;
  size?: "sm" | "lg";
  label?: string;
  className?: string;
}

/**
 * 가격을 한국 원화 포맷으로 표시한다.
 * 모든 가격 표시는 이 컴포넌트를 통해 사용할 것.
 */
export function PriceDisplay({
  amount,
  size = "sm",
  label,
  className,
}: PriceDisplayProps) {
  return (
    <div className={cn("flex flex-col", className)}>
      {label && (
        <span className="text-xs text-muted-foreground mb-0.5">{label}</span>
      )}
      <span
        className={cn(
          "font-bold tabular-nums",
          size === "lg" ? "text-2xl" : "text-base",
        )}
      >
        {formatPrice(amount)}
      </span>
    </div>
  );
}

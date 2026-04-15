import { cn } from "@/lib/utils";

export type StatusVariant =
  | "green"
  | "blue"
  | "gray"
  | "red"
  | "yellow"
  | "orange";

const variantStyles: Record<StatusVariant, string> = {
  green:  "bg-[var(--color-auction-ongoing-bg)]  text-[var(--color-auction-ongoing-text)]",
  blue:   "bg-[var(--color-auction-won-bg)]      text-[var(--color-auction-won-text)]",
  gray:   "bg-[var(--color-auction-unsold-bg)]   text-[var(--color-auction-unsold-text)]",
  red:    "bg-[var(--color-auction-canceled-bg)] text-[var(--color-auction-canceled-text)]",
  yellow: "bg-[var(--color-order-awaiting-payment-bg)] text-[var(--color-order-awaiting-payment-text)]",
  orange: "bg-[var(--color-order-not-delivered-bg)]    text-[var(--color-order-not-delivered-text)]",
};

interface StatusBadgeProps {
  label: string;
  variant: StatusVariant;
  className?: string;
}

/**
 * 모든 도메인 상태 배지의 기반 컴포넌트.
 * 색상을 직접 쓰지 말고 이 컴포넌트를 통해 사용할 것.
 */
export function StatusBadge({ label, variant, className }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        variantStyles[variant],
        className,
      )}
    >
      {label}
    </span>
  );
}

import { BadgeCheck } from "lucide-react";
import { cn } from "@/lib/utils";

interface MembershipBadgeProps {
  className?: string;
}

export function MembershipBadge({ className }: MembershipBadgeProps) {
  return (
    <BadgeCheck
      className={cn(
        "h-4 w-4 text-[var(--color-bluecheck)] fill-[var(--color-bluecheck)] stroke-white",
        className,
      )}
      aria-label="인증된 판매자"
    />
  );
}

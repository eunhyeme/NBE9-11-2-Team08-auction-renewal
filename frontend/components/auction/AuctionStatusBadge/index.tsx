import { StatusBadge } from "@/components/common/StatusBadge";
import type { AuctionStatus } from "@/types/auction";
import type { StatusVariant } from "@/components/common/StatusBadge";

const STATUS_MAP: Record<AuctionStatus, { label: string; variant: StatusVariant }> = {
  ONGOING:  { label: "경매 중",  variant: "green" },
  WON:      { label: "낙찰",    variant: "blue"  },
  UNSOLD:   { label: "유찰",    variant: "gray"  },
  CANCELED: { label: "취소됨",  variant: "red"   },
};

interface AuctionStatusBadgeProps {
  status: AuctionStatus;
  className?: string;
}

export function AuctionStatusBadge({ status, className }: AuctionStatusBadgeProps) {
  const { label, variant } = STATUS_MAP[status];
  return <StatusBadge label={label} variant={variant} className={className} />;
}

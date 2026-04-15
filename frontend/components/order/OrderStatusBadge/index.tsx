import { StatusBadge } from "@/components/common/StatusBadge";
import type { OrderStatus } from "@/types/order";
import type { StatusVariant } from "@/components/common/StatusBadge";

const STATUS_MAP: Record<OrderStatus, { label: string; variant: StatusVariant }> = {
  AWAITING_PAYMENT: { label: "결제 대기",  variant: "yellow" },
  PAID:             { label: "결제 완료",  variant: "green"  },
  DELIVERING:       { label: "배송 중",    variant: "blue"   },
  DELIVERED:        { label: "배송 완료",  variant: "green"  },
  NOT_PAID:         { label: "미결제",     variant: "red"    },
  NOT_DELIVERED:    { label: "미배송",     variant: "orange" },
};

interface OrderStatusBadgeProps {
  status: OrderStatus;
  className?: string;
}

export function OrderStatusBadge({ status, className }: OrderStatusBadgeProps) {
  const { label, variant } = STATUS_MAP[status];
  return <StatusBadge label={label} variant={variant} className={className} />;
}

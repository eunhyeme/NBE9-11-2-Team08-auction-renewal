"use client";

import { Loader2, Check, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { formatDate } from "@/lib/utils/formatDate";
import type { MembershipApplicationResponse } from "@/types/membership";

const STATUS_MAP: Record<
  string,
  { label: string; className: string }
> = {
  PENDING: {
    label: "검토 중",
    className:
      "bg-[var(--color-order-awaiting_payment-bg)] text-[var(--color-order-awaiting_payment-text)]",
  },
  APPROVED: {
    label: "승인",
    className:
      "bg-[var(--color-auction-ongoing-bg)] text-[var(--color-auction-ongoing-text)]",
  },
  REJECTED: {
    label: "거절",
    className: "bg-[var(--color-auction-canceled-bg)] text-[var(--color-auction-canceled-text)]",
  },
};

interface MembershipApplicationRowProps {
  application: MembershipApplicationResponse;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  isApproving: boolean;
  isRejecting: boolean;
}

export function MembershipApplicationRow({
  application,
  onApprove,
  onReject,
  isApproving,
  isRejecting,
}: MembershipApplicationRowProps) {
  const status = STATUS_MAP[application.status] ?? {
    label: application.status,
    className: "",
  };
  const isPending = application.status === "PENDING";
  const isLoading = isApproving || isRejecting;

  return (
    <tr className="border-b last:border-0">
      <td className="py-3 px-4 text-sm font-medium">{application.nickname}</td>
      <td className="py-3 px-4 text-sm text-muted-foreground">
        {formatDate(application.requestedAt)}
      </td>
      <td className="py-3 px-4">
        <Badge className={status.className}>{status.label}</Badge>
      </td>
      <td className="py-3 px-4 text-sm text-muted-foreground">
        {application.reason ?? "—"}
      </td>
      <td className="py-3 px-4">
        {isPending ? (
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="default"
              onClick={() => onApprove(application.id)}
              disabled={isLoading}
            >
              {isApproving ? (
                <Loader2 className="h-3 w-3 animate-spin" />
              ) : (
                <Check className="h-3 w-3" />
              )}
              승인
            </Button>
            <Button
              size="sm"
              variant="destructive"
              onClick={() => onReject(application.id)}
              disabled={isLoading}
            >
              {isRejecting ? (
                <Loader2 className="h-3 w-3 animate-spin" />
              ) : (
                <X className="h-3 w-3" />
              )}
              거절
            </Button>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">
            {application.reviewedAt ? formatDate(application.reviewedAt) : "—"}
          </span>
        )}
      </td>
    </tr>
  );
}

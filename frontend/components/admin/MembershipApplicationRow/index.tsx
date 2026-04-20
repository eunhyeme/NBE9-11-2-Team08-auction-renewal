"use client";

import { useState } from "react";
import { Loader2, Check, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
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
  onReject: (id: number, reason: string) => void;
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
  const [isRejectMode, setIsRejectMode] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
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
          isRejectMode ? (
            <div className="flex flex-col gap-2">
              <Input
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="거절 사유"
                disabled={isLoading}
                className="h-8"
              />
              <div className="flex gap-2">
                <Button
                  size="sm"
                  variant="destructive"
                  onClick={() => onReject(application.id, rejectReason)}
                  disabled={isLoading}
                >
                  {isRejecting ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <X className="h-3 w-3" />
                  )}
                  거절
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setIsRejectMode(false);
                    setRejectReason("");
                  }}
                  disabled={isLoading}
                >
                  취소
                </Button>
              </div>
            </div>
          ) : (
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
                onClick={() => setIsRejectMode(true)}
                disabled={isLoading}
              >
                <X className="h-3 w-3" />
                거절
              </Button>
            </div>
          )
        ) : (
          <span className="text-xs text-muted-foreground">
            {application.reviewedAt ? formatDate(application.reviewedAt) : "—"}
          </span>
        )}
      </td>
    </tr>
  );
}

"use client";

import { useState } from "react";
import { toast } from "sonner";

import { MembershipApplicationRow } from "@/components/admin/MembershipApplicationRow";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";
import {
  useMembershipApplications,
  useApproveMembership,
  useRejectMembership,
} from "@/lib/hooks/useMembership";
import { ApiError } from "@/types/api";

export default function AdminPage() {
  const { data: applications, isLoading, isError } = useMembershipApplications();
  const approveMutation = useApproveMembership();
  const rejectMutation = useRejectMembership();
  const [actionId, setActionId] = useState<number | null>(null);

  const handleApprove = async (id: number) => {
    setActionId(id);
    try {
      await approveMutation.mutateAsync(id);
      toast.success("인증 신청을 승인했습니다.");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("승인 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (id: number) => {
    setActionId(id);
    try {
      await rejectMutation.mutateAsync(id);
      toast.success("인증 신청을 거절했습니다.");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("거절 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setActionId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <LoadingSpinner />
      </div>
    );
  }

  if (isError) {
    return <ErrorMessage message="신청 목록을 불러오는 데 실패했습니다." />;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold">판매자 인증 신청</h1>
        <p className="text-sm text-muted-foreground mt-1">
          신청을 검토하고 승인 또는 거절해 주세요.
        </p>
      </div>

      {!applications?.length ? (
        <EmptyState message="판매자 인증 신청이 없습니다." />
      ) : (
        <div className="rounded-xl border bg-card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="py-3 px-4 text-left font-medium text-muted-foreground">
                  닉네임
                </th>
                <th className="py-3 px-4 text-left font-medium text-muted-foreground">
                  신청일시
                </th>
                <th className="py-3 px-4 text-left font-medium text-muted-foreground">
                  상태
                </th>
                <th className="py-3 px-4 text-left font-medium text-muted-foreground">
                  사유
                </th>
                <th className="py-3 px-4 text-left font-medium text-muted-foreground">
                  처리
                </th>
              </tr>
            </thead>
            <tbody>
              {applications.map((app) => (
                <MembershipApplicationRow
                  key={app.id}
                  application={app}
                  onApprove={handleApprove}
                  onReject={handleReject}
                  isApproving={
                    actionId === app.id && approveMutation.isPending
                  }
                  isRejecting={
                    actionId === app.id && rejectMutation.isPending
                  }
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

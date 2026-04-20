import { apiFetch } from "./client";
import type { ApiResponse } from "@/types/api";
import type { MembershipApplicationResponse, MembershipStatus } from "@/types/membership";

interface MembershipApplyResponse {
  applicationId: number;
  message: string;
}

interface MembershipApplicationApiResponse {
  applicationId: number;
  userId: number;
  email: string;
  nickname: string;
  status: MembershipStatus;
  reason: string | null;
  requestedAt: string;
  reviewedAt: string | null;
  reviewedBy: number | null;
}

function normalizeApplication(
  application: MembershipApplicationApiResponse,
): MembershipApplicationResponse {
  return {
    id: application.applicationId,
    userId: application.userId,
    nickname: application.nickname,
    status: application.status,
    reason: application.reason,
    requestedAt: application.requestedAt,
    reviewedAt: application.reviewedAt,
  };
}

export async function applyMembership(): Promise<MembershipApplyResponse> {
  const res = await apiFetch.post<ApiResponse<MembershipApplyResponse>>(
    "/api/membership/apply",
  );
  return res.result;
}

export async function getMembershipApplications(): Promise<
  MembershipApplicationResponse[]
> {
  const res = await apiFetch.get<ApiResponse<MembershipApplicationApiResponse[]>>(
    "/api/membership",
  );
  return res.result.map(normalizeApplication);
}

export async function approveMembership(id: number): Promise<void> {
  await apiFetch.patch(`/api/membership/${id}`, {
    status: "APPROVED",
    reason: null,
  });
}

export async function rejectMembership(id: number, reason: string): Promise<void> {
  await apiFetch.patch(`/api/membership/${id}`, {
    status: "REJECTED",
    reason,
  });
}

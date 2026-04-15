import { apiFetch } from "./client";
import type { ApiResponse } from "@/types/api";
import type { MembershipApplicationResponse } from "@/types/membership";

export async function applyMembership(): Promise<MembershipApplicationResponse> {
  const res = await apiFetch.post<ApiResponse<MembershipApplicationResponse>>(
    "/api/membership-applications",
  );
  return res.result;
}

export async function getMembershipApplications(): Promise<
  MembershipApplicationResponse[]
> {
  const res = await apiFetch.get<ApiResponse<MembershipApplicationResponse[]>>(
    "/api/membership-applications",
  );
  return res.result;
}

export async function approveMembership(id: number): Promise<void> {
  await apiFetch.post(`/api/membership-applications/${id}/approve`);
}

export async function rejectMembership(id: number): Promise<void> {
  await apiFetch.post(`/api/membership-applications/${id}/reject`);
}

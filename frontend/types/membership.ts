export type MembershipStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface MembershipApplicationResponse {
  id: number;
  userId: number;
  nickname: string;
  status: MembershipStatus;
  reason: string | null;
  requestedAt: string;
  reviewedAt: string | null;
}

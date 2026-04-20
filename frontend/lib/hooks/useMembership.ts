import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  applyMembership,
  getMembershipApplications,
  approveMembership,
  rejectMembership,
} from "@/lib/api/membership";
import { MOCK_MEMBERSHIP_APPLICATIONS } from "@/lib/mocks/data";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useMembershipApplications() {
  return useQuery({
    queryKey: ["membershipApplications"],
    queryFn: USE_MOCK
      ? () => Promise.resolve(MOCK_MEMBERSHIP_APPLICATIONS)
      : getMembershipApplications,
  });
}

export function useApplyMembership() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: USE_MOCK
      ? () =>
          Promise.resolve({
            applicationId: MOCK_MEMBERSHIP_APPLICATIONS[0].id,
            message: "판매자 인증 신청이 완료되었습니다.",
          })
      : applyMembership,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}

export function useApproveMembership() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: USE_MOCK
      ? (_: number) => Promise.resolve()
      : (id: number) => approveMembership(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["membershipApplications"] });
    },
  });
}

export function useRejectMembership() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: USE_MOCK
      ? (_: { id: number; reason: string }) => Promise.resolve()
      : ({ id, reason }: { id: number; reason: string }) =>
          rejectMembership(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["membershipApplications"] });
    },
  });
}

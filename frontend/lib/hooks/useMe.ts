import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getMe, updateMe } from "@/lib/api/user";
import { useAuthStore } from "@/lib/auth/store";
import { MOCK_ME } from "@/lib/mocks/data";
import type { ProfileUpdateRequest } from "@/types/user";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useMe() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  return useQuery({
    queryKey: ["me"],
    queryFn: USE_MOCK ? () => Promise.resolve(MOCK_ME) : getMe,
    enabled: USE_MOCK || isLoggedIn,
  });
}

export function useUpdateMe() {
  const queryClient = useQueryClient();
  const updateProfile = useAuthStore((s) => s.updateProfile);
  return useMutation({
    mutationFn: USE_MOCK
      ? (data: ProfileUpdateRequest) =>
          Promise.resolve({ ...MOCK_ME, nickname: data.nickname ?? MOCK_ME.nickname })
      : (data: ProfileUpdateRequest) => updateMe(data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ["me"] });
      updateProfile({ nickname: updated.nickname, permissions: updated.permissions });
    },
  });
}

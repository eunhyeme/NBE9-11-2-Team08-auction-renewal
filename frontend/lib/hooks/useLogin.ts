import { useMutation, useQueryClient } from "@tanstack/react-query";
import { login, getMe } from "@/lib/api/user";
import { useAuthStore } from "@/lib/auth/store";

export function useLogin() {
  const queryClient = useQueryClient();
  const loginStore = useAuthStore((s) => s.login);
  return useMutation({
    mutationFn: async ({
      email,
      password,
    }: {
      email: string;
      password: string;
    }) => {
      await login(email, password);
      return getMe();
    },
    onSuccess: (me) => {
      queryClient.setQueryData(["me"], me);
      loginStore(me);
    },
  });
}

"use client";

import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/lib/auth/store";
import { tokenManager } from "@/lib/auth/tokenManager";
import { getMe } from "@/lib/api/user";
import { MOCK_ME } from "@/lib/mocks/data";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

/**
 * 페이지 새로고침 시 refresh token 쿠키를 이용해 access token을 복원한다.
 * NEXT_PUBLIC_USE_MOCK=true 일 때는 목 유저로 자동 로그인한다.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const login = useAuthStore((s) => s.login);
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    if (USE_MOCK) {
      // 목 모드: 실제 API 호출 없이 mock 유저로 자동 로그인
      tokenManager.set("mock-access-token");
      login({
        userId: MOCK_ME.userId,
        email: MOCK_ME.email,
        nickname: MOCK_ME.nickname,
        hasBluecheck: MOCK_ME.hasBluecheck,
        role: MOCK_ME.role,
      });
      return;
    }

    (async () => {
      try {
        const res = await fetch(`${BASE_URL}/api/users/refresh`, {
          method: "POST",
          credentials: "include",
        });
        if (!res.ok) return;
        const data = await res.json();
        const { accessToken } = data.result ?? data;
        tokenManager.set(accessToken);
        const me = await getMe();
        queryClient.setQueryData(["me"], me);
        login({
          userId: me.userId,
          email: me.email,
          nickname: me.nickname,
          hasBluecheck: me.hasBluecheck,
          role: me.role,
        });
      } catch {
        // 쿠키 없음 → 미인증 상태 유지
      }
    })();
  }, [login]);

  return <>{children}</>;
}

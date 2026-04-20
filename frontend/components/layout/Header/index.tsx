"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Gavel, User, LogOut, Settings, ShieldCheck } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { MembershipBadge } from "@/components/user/MembershipBadge";
import { useAuthStore } from "@/lib/auth/store";
import { logout } from "@/lib/api/user";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

export function Header() {
  const router = useRouter();
  const { isLoggedIn, nickname, hasBluecheck, role } = useAuthStore();
  const logoutAction = useAuthStore((s) => s.logout);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 서버 오류여도 클라이언트 상태는 초기화
    } finally {
      logoutAction();
      toast.success("로그아웃 되었습니다.");
      router.push("/");
    }
  };

  return (
    <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        {/* 로고 */}
        <Link href="/" className="flex items-center gap-2 font-bold text-lg">
          <Gavel className="h-5 w-5 text-[var(--color-brand-500)]" />
          <span>경매 플랫폼</span>
        </Link>

        {/* 우측 메뉴 */}
        <div className="flex items-center gap-2">
          {isLoggedIn ? (
            <DropdownMenu>
              <DropdownMenuTrigger
                className={cn(
                  buttonVariants({ variant: "ghost", size: "sm" }),
                  "gap-2"
                )}
              >
                <Avatar className="h-7 w-7">
                  <AvatarFallback className="text-xs">
                    {nickname?.[0]?.toUpperCase() ?? "U"}
                  </AvatarFallback>
                </Avatar>
                <span className="hidden sm:inline-flex items-center gap-1">
                  {nickname}
                  {hasBluecheck && <MembershipBadge />}
                </span>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-44">
                <DropdownMenuItem
                  className="flex items-center gap-2"
                  onClick={() => router.push("/profile")}
                >
                  <User className="h-4 w-4" />
                  마이페이지
                </DropdownMenuItem>
                <DropdownMenuItem
                  className="flex items-center gap-2"
                  onClick={() => router.push("/profile/edit")}
                >
                  <Settings className="h-4 w-4" />
                  프로필 수정
                </DropdownMenuItem>
                {role === "ROLE_ADMIN" && (
                  <DropdownMenuItem
                    className="flex items-center gap-2"
                    onClick={() => router.push("/admin")}
                  >
                    <ShieldCheck className="h-4 w-4" />
                    관리자 페이지
                  </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  className="text-destructive flex items-center gap-2"
                  onClick={handleLogout}
                >
                  <LogOut className="h-4 w-4" />
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <>
              <Link
                href="/login"
                className={cn(buttonVariants({ variant: "ghost", size: "sm" }))}
              >
                로그인
              </Link>
              <Link
                href="/signup"
                className={cn(buttonVariants({ size: "sm" }))}
              >
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

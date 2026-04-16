"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { BackButton } from "@/components/common/BackButton";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { AddressInput } from "@/components/common/AddressInput";

import {
  profileUpdateSchema,
  type ProfileUpdateFormValues,
} from "@/lib/validations/userSchema";
import { useMe, useUpdateMe } from "@/lib/hooks/useMe";
import { useAuthStore } from "@/lib/auth/store";
import { ApiError } from "@/types/api";

export default function ProfileEditPage() {
  const router = useRouter();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const { data: me, isLoading } = useMe();
  const updateMe = useUpdateMe();

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<ProfileUpdateFormValues>({
    resolver: zodResolver(profileUpdateSchema),
    defaultValues: { nickname: "", address: "" },
  });

  useEffect(() => {
    if (!isLoggedIn) router.replace("/login");
  }, [isLoggedIn, router]);

  // 프로필 데이터 로드 후 폼 초기값 설정
  useEffect(() => {
    if (me) {
      reset({ nickname: me.nickname, address: me.address });
    }
  }, [me, reset]);

  if (!isLoggedIn) return null;

  if (isLoading) {
    return <LoadingSpinner className="py-24" size="lg" />;
  }

  const onSubmit = async (values: ProfileUpdateFormValues) => {
    try {
      await updateMe.mutateAsync(values);
      toast.success("프로필이 수정되었습니다.");
      router.push("/profile");
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message);
      } else {
        toast.error("프로필 수정 중 오류가 발생했습니다.");
      }
    }
  };

  return (
    <div className="flex flex-col gap-4 max-w-md mx-auto">
      <div className="self-start">
        <BackButton label="마이페이지" />
      </div>
      <Card>
        <CardHeader className="items-center">
          {/* 아바타 */}
          <Avatar className="h-20 w-20 mb-2">
            <AvatarFallback className="text-3xl">
              {me?.nickname?.[0]?.toUpperCase() ?? "U"}
            </AvatarFallback>
          </Avatar>
          <CardTitle>프로필 수정</CardTitle>
        </CardHeader>

        <Separator />

        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="flex flex-col gap-5 pt-6">
            {/* 이메일 (수정 불가) */}
            <div className="flex flex-col gap-1.5">
              <Label className="text-muted-foreground">이메일</Label>
              <Input value={me?.email ?? ""} disabled className="bg-muted" />
            </div>

            {/* 닉네임 */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="nickname">닉네임</Label>
              <Input
                id="nickname"
                placeholder="2~20자"
                {...register("nickname")}
              />
              {errors.nickname && (
                <p className="text-xs text-destructive">{errors.nickname.message}</p>
              )}
            </div>

            {/* 주소 */}
            <div className="flex flex-col gap-1.5">
              <Label>주소</Label>
              <Controller
                control={control}
                name="address"
                render={({ field }) => (
                  <AddressInput
                    value={field.value}
                    onChange={field.onChange}
                    error={errors.address?.message}
                  />
                )}
              />
            </div>

            <Button
              type="submit"
              className="w-full mt-2"
              disabled={isSubmitting || !isDirty}
            >
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              수정 완료
            </Button>
          </CardContent>
        </form>
      </Card>
    </div>
  );
}

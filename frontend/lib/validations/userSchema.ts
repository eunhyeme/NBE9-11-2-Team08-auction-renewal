import { z } from "zod";

export const signupSchema = z.object({
  email: z.string().email({ error: "유효한 이메일 주소를 입력해 주세요." }),
  password: z
    .string()
    .min(8, { error: "비밀번호는 8자 이상이어야 합니다." }),
  nickname: z
    .string()
    .min(2, { error: "닉네임은 2자 이상이어야 합니다." })
    .max(20, { error: "닉네임은 20자 이하여야 합니다." }),
  address: z.string().min(1, { error: "주소를 입력해 주세요." }),
});

export const loginSchema = z.object({
  email: z.string().email({ error: "유효한 이메일 주소를 입력해 주세요." }),
  password: z.string().min(1, { error: "비밀번호를 입력해 주세요." }),
});

export const profileUpdateSchema = z.object({
  nickname: z
    .string()
    .min(2, { error: "닉네임은 2자 이상이어야 합니다." })
    .max(20, { error: "닉네임은 20자 이하여야 합니다." }),
  address: z.string().min(1, { error: "주소를 입력해 주세요." }),
});

export type SignupFormValues = z.infer<typeof signupSchema>;
export type LoginFormValues = z.infer<typeof loginSchema>;
export type ProfileUpdateFormValues = z.infer<typeof profileUpdateSchema>;

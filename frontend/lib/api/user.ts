import { apiFetch } from "./client";
import { tokenManager } from "@/lib/auth/tokenManager";
import type { ApiResponse } from "@/types/api";
import type {
  LoginResponse,
  SignupRequest,
  UserMeResponse,
  ProfileUpdateRequest,
} from "@/types/user";

export async function signup(data: SignupRequest): Promise<void> {
  await apiFetch.post("/api/users/signup", data);
}

export async function login(
  email: string,
  password: string,
): Promise<LoginResponse> {
  const res = await apiFetch.post<ApiResponse<LoginResponse>>(
    "/api/users/login",
    { email, password },
  );
  tokenManager.set(res.result.accessToken);
  return res.result;
}

export async function logout(): Promise<void> {
  await apiFetch.post("/api/users/logout");
  tokenManager.clear();
}

export async function getMe(): Promise<UserMeResponse> {
  const res = await apiFetch.get<ApiResponse<UserMeResponse>>("/api/users/me");
  return res.result;
}

export async function updateMe(
  data: ProfileUpdateRequest,
): Promise<UserMeResponse> {
  const res = await apiFetch.put<ApiResponse<UserMeResponse>>(
    "/api/users/me",
    data,
  );
  return res.result;
}

export type UserRole = "ROLE_USER" | "ROLE_ADMIN";

export interface UserMeResponse {
  userId: number;
  email: string;
  nickname: string;
  address: string;
  permissions: string[];
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  address: string;
}

export interface ProfileUpdateRequest {
  nickname: string;
  address: string;
}

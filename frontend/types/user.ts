export type UserRole = "ROLE_USER" | "ROLE_ADMIN";

export interface UserMeResponse {
  userId: number;
  email: string;
  nickname: string;
  address: string;
  hasBluecheck: boolean;
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  userId: number;
  email: string;
  nickname: string;
  hasBluecheck: boolean;
  role: UserRole;
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

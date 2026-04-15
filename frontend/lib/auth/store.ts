"use client";

import { create } from "zustand";
import type { UserRole } from "@/types/user";

interface AuthState {
  userId: number | null;
  email: string | null;
  nickname: string | null;
  hasBluecheck: boolean;
  role: UserRole | null;
  isLoggedIn: boolean;

  login: (user: {
    userId: number;
    email: string;
    nickname: string;
    hasBluecheck: boolean;
    role: UserRole;
  }) => void;
  logout: () => void;
  updateProfile: (data: {
    nickname: string;
    hasBluecheck?: boolean;
  }) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  userId: null,
  email: null,
  nickname: null,
  hasBluecheck: false,
  role: null,
  isLoggedIn: false,

  login: (user) =>
    set({
      ...user,
      isLoggedIn: true,
    }),

  logout: () =>
    set({
      userId: null,
      email: null,
      nickname: null,
      hasBluecheck: false,
      role: null,
      isLoggedIn: false,
    }),

  updateProfile: (data) =>
    set((state) => ({
      nickname: data.nickname,
      hasBluecheck: data.hasBluecheck ?? state.hasBluecheck,
    })),
}));

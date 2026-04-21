"use client";

import { create } from "zustand";
import type { UserRole } from "@/types/user";

interface AuthState {
  userId: number | null;
  email: string | null;
  nickname: string | null;
  permissions: string[];
  role: UserRole | null;
  isLoggedIn: boolean;

  login: (user: {
    userId: number;
    email: string;
    nickname: string;
    permissions: string[];
    role: UserRole;
  }) => void;
  logout: () => void;
  updateProfile: (data: {
    nickname: string;
    permissions?: string[];
  }) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  userId: null,
  email: null,
  nickname: null,
  permissions: [],
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
      permissions: [],
      role: null,
      isLoggedIn: false,
    }),

  updateProfile: (data) =>
    set((state) => ({
      nickname: data.nickname,
      permissions: data.permissions ?? state.permissions,
    })),
}));

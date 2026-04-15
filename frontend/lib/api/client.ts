import { ApiError } from "@/types/api";
import { tokenManager } from "@/lib/auth/tokenManager";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type FetchOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

async function doRefresh(): Promise<string | null> {
  try {
    const res = await fetch(`${BASE_URL}/api/users/refresh`, {
      method: "POST",
      credentials: "include",
    });
    if (!res.ok) return null;
    const data = await res.json();
    const newToken: string = data.result?.accessToken ?? data.accessToken;
    tokenManager.set(newToken);
    return newToken;
  } catch {
    return null;
  }
}

async function getValidToken(): Promise<string | null> {
  const token = tokenManager.get();
  if (token) return token;

  if (isRefreshing) return refreshPromise;

  isRefreshing = true;
  refreshPromise = doRefresh().finally(() => {
    isRefreshing = false;
    refreshPromise = null;
  });
  return refreshPromise;
}

async function fetchWithAuth(
  url: string,
  options: FetchOptions = {},
): Promise<Response> {
  const { body, ...rest } = options;

  const buildHeaders = (token: string | null): HeadersInit => ({
    "Content-Type": "application/json",
    ...(rest.headers as Record<string, string>),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  });

  const buildInit = (token: string | null): RequestInit => ({
    ...rest,
    credentials: "include",
    headers: buildHeaders(token),
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });

  const token = await getValidToken();
  const response = await fetch(`${BASE_URL}${url}`, buildInit(token));

  if (response.status === 401) {
    // 토큰 만료 → 강제 갱신 후 1회 재시도
    tokenManager.clear();
    const newToken = await doRefresh();
    if (!newToken) {
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
      throw new ApiError("UNAUTHORIZED", "로그인이 필요합니다.", 401);
    }
    return fetch(`${BASE_URL}${url}`, buildInit(newToken));
  }

  return response;
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let code = "UNKNOWN_ERROR";
    let message = `HTTP ${res.status}`;
    try {
      const data = await res.json();
      code = data.code ?? code;
      message = data.message ?? message;
    } catch {
      // JSON 파싱 실패는 무시
    }
    throw new ApiError(code, message, res.status);
  }
  // 204 No Content
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const apiFetch = {
  get: <T>(url: string, options?: Omit<FetchOptions, "method" | "body">) =>
    fetchWithAuth(url, { ...options, method: "GET" }).then((r) =>
      handleResponse<T>(r),
    ),

  post: <T>(url: string, body?: unknown, options?: Omit<FetchOptions, "method">) =>
    fetchWithAuth(url, { ...options, method: "POST", body }).then((r) =>
      handleResponse<T>(r),
    ),

  put: <T>(url: string, body?: unknown, options?: Omit<FetchOptions, "method">) =>
    fetchWithAuth(url, { ...options, method: "PUT", body }).then((r) =>
      handleResponse<T>(r),
    ),

  delete: <T>(url: string, options?: Omit<FetchOptions, "method" | "body">) =>
    fetchWithAuth(url, { ...options, method: "DELETE" }).then((r) =>
      handleResponse<T>(r),
    ),
};

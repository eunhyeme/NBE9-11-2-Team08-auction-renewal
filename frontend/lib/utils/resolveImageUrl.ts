const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function resolveImageUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  if (url.startsWith("http")) return url;
  return `${BASE_URL}${url}`;
}

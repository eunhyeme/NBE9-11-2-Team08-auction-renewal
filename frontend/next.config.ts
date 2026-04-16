import type { NextConfig } from "next";

const isDev = process.env.NODE_ENV === "development";

const nextConfig: NextConfig = {
  images: {
    unoptimized: isDev,
    remotePatterns: [
      // TODO: 목 데이터 제거 시 함께 삭제 (mock 이미지용 임시 허용)
      new URL("https://images.unsplash.com/**"),
      new URL("http://localhost:8080/uploads/**"),
    ],
  },
};

export default nextConfig;

# 경매 플랫폼 — Frontend

## 빠른 시작

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

백엔드 없이 UI 확인: `.env.local`에서 `NEXT_PUBLIC_USE_MOCK=true` (기본값 on)  
백엔드 연동 시: `NEXT_PUBLIC_USE_MOCK=false`로 변경

## 기술 스택

Next.js 16 · React 19 · Tailwind CSS v4 · shadcn/ui (Base UI) · TanStack Query v5 · Zustand v5 · React Hook Form + Zod v4

## 개발 가이드

컴포넌트 추가, API 연동, 주의사항 등 → **[CONTRIBUTING.md](./CONTRIBUTING.md)**

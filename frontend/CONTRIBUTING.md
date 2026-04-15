# 개발 가이드

## 목차

1. [왜 이 라이브러리를 쓰는가](#1-왜-이-라이브러리를-쓰는가)
2. [버전 주의사항 — 인터넷 예제와 다른 점](#2-버전-주의사항--인터넷-예제와-다른-점)
3. [디렉토리 구조](#3-디렉토리-구조)
4. [팀 규칙](#4-팀-규칙)
5. [새 컴포넌트 추가](#5-새-컴포넌트-추가)
6. [새 페이지 추가](#6-새-페이지-추가)
7. [새 API 연동](#7-새-api-연동)
8. [새 폼 추가](#8-새-폼-추가)
9. [디자인 토큰](#9-디자인-토큰)
10. [공통 컴포넌트 사용법](#10-공통-컴포넌트-사용법)
11. [목 데이터](#11-목-데이터)
12. [인증](#12-인증)

---

## 1. 왜 이 라이브러리를 쓰는가

### TanStack Query — API 데이터 관리

API에서 가져온 데이터를 관리합니다. 직접 `fetch`를 쓰면 매 컴포넌트마다 이걸 반복해야 합니다:

```tsx
// TanStack Query 없이
const [data, setData] = useState(null);
const [isLoading, setIsLoading] = useState(false);
const [error, setError] = useState(null);

useEffect(() => {
  setIsLoading(true);
  fetch("/api/auctions")
    .then(res => res.json())
    .then(data => { setData(data); setIsLoading(false); })
    .catch(err => { setError(err); setIsLoading(false); });
}, []);
```

TanStack Query를 쓰면:

```tsx
const { data, isLoading, isError } = useAuctions();
```

로딩 상태, 에러 처리, **캐싱**, 중복 요청 제거, 백그라운드 갱신을 자동으로 처리합니다.

---

### Zustand — 클라이언트 전역 상태

로그인 정보처럼 **API와 무관하게 앱 전체에서 공유해야 하는 상태**를 관리합니다.

React Context로도 가능하지만, Context는 값이 하나라도 바뀌면 해당 Context를 구독하는 컴포넌트 **전체**가 리렌더링됩니다. Zustand는 실제로 사용하는 값이 바뀔 때만 리렌더링합니다.

```ts
// nickname만 쓰는 컴포넌트는 role이 바뀌어도 리렌더링 안 됨
const nickname = useAuthStore((s) => s.nickname);
```

Redux 대비 보일러플레이트가 거의 없어서 코드가 단순합니다.

---

### React Hook Form — 폼 상태 관리

폼도 상태입니다. 필드 값, 에러, 제출 중 여부를 직접 관리하면:

```tsx
// 필드가 10개면 아래 같은 코드가 10세트
const [title, setTitle] = useState("");
const [titleError, setTitleError] = useState("");
const [isSubmitting, setIsSubmitting] = useState(false);
```

React Hook Form은 이걸 `register`, `errors`, `isSubmitting` 세 가지로 처리합니다. 또한 입력할 때마다 리렌더링하지 않아서 성능도 좋습니다.

---

### Zod — 검증 스키마 + 타입 자동 생성

폼 검증 규칙과 TypeScript 타입을 **하나의 스키마**에서 관리합니다.

```ts
const schema = z.object({
  title: z.string().min(2, { error: "2자 이상 입력해 주세요." }),
  startPrice: z.coerce.number().min(1000, { error: "1,000원 이상" }),
});

// 스키마에서 타입이 자동 생성 — 따로 interface 작성 불필요
type FormValues = z.infer<typeof schema>;
// → { title: string; startPrice: number }
```

검증 규칙과 타입을 따로 관리하면 둘이 어긋날 수 있는데, Zod는 하나에서 둘 다 나옵니다.  
`@hookform/resolvers`가 Zod 스키마를 React Hook Form에 연결해 줍니다.

---

### 한 줄 요약

| 라이브러리 | 해결하는 문제 |
|------|------|
| TanStack Query | API 데이터 로딩·캐싱·에러 자동 처리 |
| Zustand | 앱 전체 공유 상태 (로그인 정보 등) |
| React Hook Form | 폼 필드·에러·제출 상태 관리 |
| Zod | 검증 규칙 작성 + TypeScript 타입 자동 생성 |

---

## 2. 버전 주의사항 — 인터넷 예제와 다른 점

이 프로젝트는 최신 버전을 쓰기 때문에 인터넷에서 찾은 코드를 그대로 붙여넣으면 동작하지 않는 경우가 있습니다.

### Next.js 16 — params가 Promise

```tsx
// ❌ 구버전 방식 (에러 발생)
export default function Page({ params }: { params: { id: string } }) {
  const id = params.id;
}

// ✅ Next.js 16
import { use } from "react";
export default function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
}
```

### shadcn/ui — asChild 없음

우리 프로젝트의 shadcn은 **Base UI** 기반입니다. 인터넷 예제 대부분은 구버전인 **Radix UI** 기반이라 `asChild`를 씁니다. 우리는 `render` prop을 씁니다.

```tsx
// ❌ Radix UI 방식 (우리 프로젝트에서 작동 안 함)
<DialogClose asChild>
  <Button>닫기</Button>
</DialogClose>

// ✅ Base UI 방식
<DialogClose render={<Button variant="outline" />}>
  닫기
</DialogClose>
```

버튼처럼 보이는 링크가 필요할 때:

```tsx
// ❌ asChild 없음
<Button asChild><Link href="/login">로그인</Link></Button>

// ✅ buttonVariants를 Link에 직접 적용
import { buttonVariants } from "@/components/ui/button";
<Link href="/login" className={cn(buttonVariants({ variant: "ghost" }))}>
  로그인
</Link>
```

### button 안에 button 넣지 말 것

`DialogClose`, `DropdownMenuTrigger` 등 Base UI의 트리거 컴포넌트는 자체적으로 `<button>`을 렌더링합니다. 그 안에 `<Button>` 컴포넌트를 또 넣으면 `<button>` 안에 `<button>`이 되어 hydration 에러가 납니다.

```tsx
// ❌ 중첩 버튼 — hydration 에러
<DialogClose>
  <Button>취소</Button>
</DialogClose>

// ✅ render prop으로 하나로 합치기
<DialogClose render={<Button variant="outline" />}>
  취소
</DialogClose>

// ✅ DropdownMenuTrigger도 마찬가지
<DropdownMenuTrigger className={cn(buttonVariants({ variant: "ghost" }), "gap-2")}>
  메뉴 열기
</DropdownMenuTrigger>
```

### Zod v4 — 에러 메시지 문법

```ts
// ❌ v3 문법 (타입 에러 발생)
z.string().min(2, { message: "2자 이상" })

// ✅ v4 문법
z.string().min(2, { error: "2자 이상" })
z.coerce.number().min(1000, { error: "1,000원 이상" })
```

---

## 3. 디렉토리 구조

```
frontend/
├── app/
│   ├── (auth)/          # 레이아웃 없는 인증 페이지 (login, signup)
│   ├── (main)/          # Header + Footer 포함 페이지
│   ├── admin/           # 관리자 전용 (ROLE_ADMIN)
│   ├── layout.tsx       # 루트 레이아웃 (Providers, Toaster)
│   └── middleware.ts    # 접근 제어
│
├── components/
│   ├── ui/              # shadcn 자동 생성 — 직접 수정 금지
│   ├── common/          # 도메인 무관 범용 컴포넌트
│   ├── auction/         # 경매 관련
│   ├── order/           # 주문 관련
│   ├── user/            # 사용자 관련
│   ├── mypage/          # 마이페이지 탭/필터
│   ├── admin/           # 관리자 전용
│   └── layout/          # Header, Footer
│
├── lib/
│   ├── api/             # fetch 함수 — 컴포넌트에서 직접 import 금지
│   │   └── client.ts    # fetch 래퍼 (JWT 자동 갱신, 401 처리)
│   ├── auth/            # tokenManager(메모리), Zustand store
│   ├── hooks/           # TanStack Query 훅 — 컴포넌트는 여기서만 가져옴
│   ├── mocks/data.ts    # 목 데이터
│   ├── utils/           # formatPrice, formatDate, auctionHelpers
│   └── validations/     # Zod 스키마
│
├── providers/           # QueryProvider, AuthProvider
└── types/               # TypeScript 타입 정의
```

---

## 4. 팀 규칙

| 규칙 | 이유 |
|------|------|
| 컴포넌트에서 `lib/api/*` 직접 import 금지 | `lib/hooks/*` 훅만 사용 |
| 가격은 반드시 `<PriceDisplay />` 사용 | `toLocaleString()` 직접 사용 금지 |
| 상태 표시는 `*StatusBadge` 컴포넌트 사용 | 디자인 토큰 일관성 |
| `components/ui/` 파일 직접 수정 금지 | shadcn 재생성 시 덮어쓰임 |
| hex 색상 직접 작성 금지 | `var(--color-*)` CSS 변수 사용 |

---

## 5. 새 컴포넌트 추가

`components/auction/BidCountBadge/` 를 만드는 예시입니다.

### Step 1. 파일 생성

```
components/auction/BidCountBadge/index.tsx
```

shadcn 컴포넌트처럼 폴더 하나에 `index.tsx` 하나로 구성합니다.

### Step 2. 컴포넌트 작성

```tsx
// components/auction/BidCountBadge/index.tsx

interface BidCountBadgeProps {
  count: number;
}

export function BidCountBadge({ count }: BidCountBadgeProps) {
  return (
    <span className="text-xs text-muted-foreground">
      입찰 {count}회
    </span>
  );
}
```

**체크리스트:**
- 상태가 없으면 `"use client"` 생략 (서버 컴포넌트로 두기)
- 색상은 `var(--color-*)` 사용, hex 직접 작성 금지
- 가격 표시가 필요하면 `<PriceDisplay />` 사용
- 공통 컴포넌트(`EmptyState`, `LoadingSpinner` 등) 재사용

### Step 3. 사용

```tsx
import { BidCountBadge } from "@/components/auction/BidCountBadge";

<BidCountBadge count={auction.bidCount} />
```

### 클라이언트 컴포넌트가 필요한 경우

`useState`, `useEffect`, 이벤트 핸들러, 훅을 사용하면 파일 맨 위에 `"use client"` 추가:

```tsx
"use client";

import { useState } from "react";

export function ExpandableDescription({ text }: { text: string }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div>
      <p className={expanded ? "" : "line-clamp-3"}>{text}</p>
      <button
        onClick={() => setExpanded((v) => !v)}
        className="text-xs text-muted-foreground mt-1"
      >
        {expanded ? "접기" : "더 보기"}
      </button>
    </div>
  );
}
```

---

## 6. 새 페이지 추가

```tsx
// app/(main)/auctions/[auctionId]/bids/page.tsx
"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/auth/store";

interface Props {
  params: Promise<{ auctionId: string }>;
}

export default function BidHistoryPage({ params }: Props) {
  const { auctionId } = use(params);   // ← Next.js 16 필수
  const { isLoggedIn } = useAuthStore();
  const router = useRouter();

  // 인증 필요한 페이지라면
  if (!isLoggedIn) {
    router.replace("/login");
    return null;
  }

  return (
    <div className="max-w-2xl mx-auto flex flex-col gap-6">
      <h1 className="text-2xl font-bold">입찰 내역</h1>
      {/* 컴포넌트 조합 */}
    </div>
  );
}
```

접근 제어가 필요하면 `middleware.ts`에 경로 추가:

```ts
// middleware.ts — 보호할 경로에 패턴 추가
const protectedRoutes = ["/orders/:path*", "/profile/:path*", "/auctions/new"];
```

---

## 7. 새 API 연동

### 7-1. 타입 정의

```ts
// types/bid.ts
export interface BidResponse {
  bidId: number;
  auctionId: number;
  bidderId: number;
  bidPrice: number;
  createdAt: string;
}
```

### 7-2. API 함수

```ts
// lib/api/bid.ts
import { apiFetch } from "./client";
import type { ApiResponse } from "@/types/api";
import type { BidResponse } from "@/types/bid";

export async function getBids(auctionId: number): Promise<BidResponse[]> {
  const res = await apiFetch.get<ApiResponse<BidResponse[]>>(
    `/api/auctions/${auctionId}/bids`
  );
  return res.result;
}
```

### 7-3. 훅

```ts
// lib/hooks/useBids.ts
import { useQuery } from "@tanstack/react-query";
import { getBids } from "@/lib/api/bid";
import { MOCK_BIDS } from "@/lib/mocks/data";

const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export function useBids(auctionId: number) {
  return useQuery({
    queryKey: ["bids", auctionId],
    queryFn: USE_MOCK
      ? () => Promise.resolve(MOCK_BIDS)
      : () => getBids(auctionId),
    enabled: !!auctionId,
  });
}
```

### 7-4. 목 데이터

```ts
// lib/mocks/data.ts 에 추가
export const MOCK_BIDS: BidResponse[] = [
  { bidId: 1, auctionId: 1, bidderId: 5, bidPrice: 200000, createdAt: "..." },
  { bidId: 2, auctionId: 1, bidderId: 3, bidPrice: 220000, createdAt: "..." },
];
```

### 7-5. 컴포넌트에서 사용

```tsx
// 컴포넌트에서 훅만 사용 (lib/api 직접 import 금지)
const { data: bids, isLoading, isError } = useBids(auctionId);
```

---

## 8. 새 폼 추가

### 8-1. Zod 스키마

```ts
// lib/validations/bidSchema.ts
import { z } from "zod";

export const bidSchema = z.object({
  bidPrice: z.coerce
    .number({ error: "숫자를 입력해 주세요." })
    .min(1000, { error: "최소 1,000원 이상 입력해 주세요." }),
});

export type BidFormValues = z.infer<typeof bidSchema>;
```

### 8-2. 폼 컴포넌트

```tsx
"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { bidSchema, type BidFormValues } from "@/lib/validations/bidSchema";

interface BidFormProps {
  onSubmit: (price: number) => Promise<void>;
}

export function BidForm({ onSubmit }: BidFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BidFormValues>({
    resolver: zodResolver(bidSchema) as any, // z.coerce 사용 시 as any 필요
  });

  return (
    <form onSubmit={handleSubmit((v) => onSubmit(v.bidPrice))} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="bidPrice">입찰 금액</Label>
        <Input id="bidPrice" type="number" {...register("bidPrice")} />
        {errors.bidPrice && (
          <p className="text-xs text-destructive">{errors.bidPrice.message}</p>
        )}
      </div>
      <Button type="submit" disabled={isSubmitting}>
        입찰하기
      </Button>
    </form>
  );
}
```

---

## 9. 디자인 토큰

`app/globals.css`의 `@theme inline` 블록에 정의된 CSS 변수입니다. hex를 직접 쓰지 말고 이걸 사용하세요.

```css
/* 브랜드 */
--color-brand-500      /* 주 액션 (골드) */
--color-brand-600
--color-brand-700

/* 경매 상태 */
--color-auction-ongoing-bg,  -text,  -ring
--color-auction-won-bg,      -text,  -ring
--color-auction-unsold-bg,   -text,  -ring
--color-auction-canceled-bg, -text,  -ring

/* 주문 상태 */
--color-order-awaiting_payment-bg,  -text
--color-order-paid-bg,              -text
--color-order-delivering-bg,        -text
--color-order-delivered-bg,         -text
--color-order-not_paid-bg,          -text
--color-order-not_delivered-bg,     -text

/* 블루체크 */
--color-bluecheck
```

```tsx
// 사용 예
<span className="bg-[var(--color-auction-ongoing-bg)] text-[var(--color-auction-ongoing-text)] px-2 py-0.5 rounded-full text-xs">
  경매 중
</span>
```

---

## 10. 공통 컴포넌트 사용법

```tsx
// 가격 — 항상 이걸 사용, toLocaleString() 직접 사용 금지
<PriceDisplay amount={150000} label="현재가" size="lg" />
// size: "sm" | "lg" (기본값 "sm")

// 경매 상태 배지
<AuctionStatusBadge status="ONGOING" />   // 경매 중
<AuctionStatusBadge status="WON" />       // 낙찰
<AuctionStatusBadge status="UNSOLD" />    // 유찰
<AuctionStatusBadge status="CANCELED" /> // 취소

// 주문 상태 배지
<OrderStatusBadge status="AWAITING_PAYMENT" />  // 결제 대기
<OrderStatusBadge status="DELIVERING" />         // 배송 중

// 카운트다운 타이머
<CountdownTimer endedAt="2025-04-16T12:00:00Z" />
// → "3시간 22분 남음" / "마감됨"

// 뒤로가기 버튼
<BackButton label="주문 상세" href="/orders/101" />
// href 없으면 router.back()

// 빈 목록
<EmptyState message="경매 내역이 없습니다." />

// 에러
<ErrorMessage message="데이터를 불러오지 못했습니다." />

// 로딩
<LoadingSpinner size="lg" className="py-24" />
```

---

## 11. 목 데이터

```bash
# .env.local
NEXT_PUBLIC_USE_MOCK=true   # API 없이 개발
NEXT_PUBLIC_USE_MOCK=false  # 실제 백엔드 사용
```

목 모드 동작:
- 모든 쿼리가 `lib/mocks/data.ts`의 데이터를 즉시 반환
- `AuthProvider`가 mock 유저로 자동 로그인 (닉네임: 데모유저, bluecheck 보유)
- 결제·입찰 등 mutation도 성공으로 처리

데이터 변경: `lib/mocks/data.ts`의 `MOCK_AUCTIONS`, `MOCK_ORDERS`, `MOCK_ME` 등 수정.

---

## 12. 인증

```
access token  → 메모리 저장 (tokenManager.ts) — XSS 방어
refresh token → HttpOnly 쿠키 — 브라우저 자동 전송
```

새로고침 시: `AuthProvider`가 `/api/users/refresh` 호출 → access token 복원  
401 응답 시: `lib/api/client.ts`가 자동으로 갱신 후 원래 요청 재시도

```ts
// 인증 상태 읽기
const { isLoggedIn, nickname, hasBluecheck, role, userId } = useAuthStore();

// bluecheck(판매자 인증) 필요한 페이지
if (!isLoggedIn || !hasBluecheck) {
  router.replace("/profile");
  return null;
}

// ROLE_ADMIN 필요한 페이지는 middleware.ts에서 처리
```

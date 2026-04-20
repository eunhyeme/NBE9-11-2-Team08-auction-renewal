import { apiFetch } from "./client";
import { tokenManager } from "../auth/tokenManager"; // 💡 토큰 매니저 임포트 추가
import type { ApiResponse, Page } from "@/types/api";
import type { AuctionResponse } from "@/types/auction";
import type { AuctionFormValues } from "../validations/auctionSchema";

/**
 * 경매 목록 조회 (팀원 작업분 유지)
 */
export async function getAuctions(
  page = 0,
  size = 10,
  keyword?: string,
  sort = "startedAt,desc",
  minPrice?: number,
  maxPrice?: number,
  bluecheckOnly?: boolean,
): Promise<Page<AuctionResponse>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort,
  });
  
  if (keyword) params.set("keyword", keyword);
  if (minPrice !== undefined) params.set("minPrice", String(minPrice));
  if (maxPrice !== undefined) params.set("maxPrice", String(maxPrice));
  if (bluecheckOnly) params.set("bluecheckOnly", "true");

  const res = await apiFetch.get<ApiResponse<Page<AuctionResponse>>>(
    `/api/auctions?${params.toString()}`,
  );
  return res.result;
}

/**
 * 경매 상세 조회 (유지)
 */
export async function getAuction(auctionId: number): Promise<AuctionResponse> {
  const res = await apiFetch.get<ApiResponse<AuctionResponse>>(`/api/auctions/${auctionId}`);
  return res.result;
}

/**
 * 경매 등록 (시니어 리뷰 반영: 에러 핸들링 및 시간 검증 최적화)
 */
export async function createAuction(data: AuctionFormValues): Promise<AuctionResponse> {
  const formData = new FormData();
  
  // 1. 시간 설정 (기존 우량 로직 유지)
  const now = new Date();
  const startedAt = new Date(now.getTime() + 60 * 1000); 
  startedAt.setSeconds(0);
  startedAt.setMilliseconds(0);

  const endedAt = new Date(startedAt.getTime() + data.duration * 24 * 60 * 60 * 1000);

  const toLocalISOString = (date: Date) => {
    const pad = (num: number) => String(num).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  };

  const requestDto = {
    title: data.title,
    description: data.description,
    startPrice: data.startPrice,
    bidUnit: data.bidUnit,
    startedAt: toLocalISOString(startedAt),
    endedAt: toLocalISOString(endedAt),
  };

  formData.append(
    "request",
    new Blob([JSON.stringify(requestDto)], { type: "application/json" })
  );

  // data.image가 있을 때만 append 하도록 체크
  if (data.image) {
    formData.append("image", data.image);
  }

  const token = tokenManager.get();
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "";

  // 2. 에러 핸들링 강화 (시니어 리뷰 1번 적용)
  try {
    const res = await fetch(`${baseUrl}/api/auctions`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (!res.ok) {
      let errorMessage = "알 수 없는 오류가 발생했습니다.";
      try {
        // 백엔드의 ApiResponse 구조에서 message 추출 시도
        const errorResult: ApiResponse<any> = await res.json();
        errorMessage = errorResult.message || `HTTP error! status: ${res.status}`;
      } catch (jsonError) {
        // JSON 파싱 실패 시 (예: 서버가 HTML 에러 페이지를 반환할 때)
        errorMessage = `응답 파싱 오류: ${res.statusText || res.status}`;
      }
      throw new Error(errorMessage);
    }

    const result: ApiResponse<AuctionResponse> = await res.json();
    return result.result;

  } catch (error) {
    console.error("Auction creation failed:", error);
    if (error instanceof Error) {
      throw error; // 파싱된 구체적인 에러 메시지 전파
    } else {
      throw new Error("경매 등록 중 예상치 못한 오류가 발생했습니다.");
    }
  }
}

/**
 * 경매 상품 철회 (삭제)
 * @param auctionId 철회할 경매의 ID
 */
export async function deleteAuction(auctionId: number): Promise<void> {
  const token = tokenManager.get();
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "";

  try {
    const res = await fetch(`${baseUrl}/api/auctions/${auctionId}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!res.ok) {
      let errorMessage = "경매 철회에 실패했습니다.";
      try {
        const errorResult = await res.json();
        errorMessage = errorResult.message || errorMessage;
      } catch (e) {
        // JSON 파싱 실패 시 기본 메시지 유지
      }
      throw new Error(errorMessage);
    }
  } catch (error) {
    if (error instanceof Error) {
      throw error;
    } else {
      throw new Error("경매 철회 중 예상치 못한 오류가 발생했습니다.");
    }
  }
}

/**
 * 경매 상품 수정 (PATCH)
 */
export async function updateAuction(
  auctionId: number, 
  data: Partial<AuctionFormValues>
): Promise<AuctionResponse> {
  const formData = new FormData();
  
  // 1. 수정 데이터 구성 (Partial 타입이므로 값이 있는 경우만 보냄)
  const requestDto: any = {
    title: data.title,
    description: data.description,
    durationInDays: data.duration,
    startPrice: data.startPrice,
    bidUnit: data.bidUnit
  };

  formData.append(
    "request",
    new Blob([JSON.stringify(requestDto)], { type: "application/json" })
  );

  // 💡 2. 이미지가 새로 선택된 경우에만 추가 (타입 에러 방지 및 로직 최적화)
  if (data.image) {
    formData.append("image", data.image);
  }

  const token = tokenManager.get();
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "";

  try {
    const res = await fetch(`${baseUrl}/api/auctions/${auctionId}`, {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (!res.ok) {
      const errorResult: ApiResponse<any> = await res.json();
      throw new Error(errorResult.message || "경매 수정에 실패했습니다.");
    }

    const result: ApiResponse<AuctionResponse> = await res.json();
    return result.result;
  } catch (error) {
    if (error instanceof Error) throw error;
    throw new Error("경매 수정 중 예상치 못한 오류가 발생했습니다.");
  }
}

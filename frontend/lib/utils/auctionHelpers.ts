import { intervalToDuration, isPast, parseISO } from "date-fns";

/**
 * 경매 종료까지 남은 시간을 사람이 읽기 쉬운 문자열로 반환한다.
 * 예: "3시간 22분 남음" / "종료됨"
 */
export function formatTimeLeft(endedAt: string): string {
  const end = parseISO(endedAt);
  if (isPast(end)) return "종료됨";

  const duration = intervalToDuration({ start: new Date(), end });
  const parts: string[] = [];
  if (duration.days) parts.push(`${duration.days}일`);
  if (duration.hours) parts.push(`${duration.hours}시간`);
  if (duration.minutes) parts.push(`${duration.minutes}분`);
  if (!parts.length) parts.push("1분 미만");

  return `${parts.join(" ")} 남음`;
}

/**
 * 다음 최소 입찰 가능 금액을 계산한다.
 */
export function calcMinBid(currentPrice: number, bidUnit: number): number {
  return currentPrice + bidUnit;
}

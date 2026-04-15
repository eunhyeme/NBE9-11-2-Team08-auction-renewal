import { format, parseISO } from "date-fns";
import { ko } from "date-fns/locale";

/**
 * ISO 문자열을 한국어 날짜 포맷으로 변환한다.
 * 예: "2025-04-15T14:30:00" → "2025.04.15 14:30"
 */
export function formatDate(isoString: string): string {
  return format(parseISO(isoString), "yyyy.MM.dd HH:mm", { locale: ko });
}

/**
 * ISO 문자열을 날짜만 표시한다.
 * 예: "2025-04-15T14:30:00" → "2025.04.15"
 */
export function formatDateOnly(isoString: string): string {
  return format(parseISO(isoString), "yyyy.MM.dd", { locale: ko });
}
